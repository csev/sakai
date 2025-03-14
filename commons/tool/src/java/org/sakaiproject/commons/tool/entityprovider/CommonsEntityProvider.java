/**
 * Copyright (c) 2003-2017 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.commons.tool.entityprovider;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.sakaiproject.commons.api.CommonsEvents;
import org.sakaiproject.commons.api.CommonsManager;
import org.sakaiproject.commons.api.CommonsSecurityManager;
import org.sakaiproject.commons.api.PostReferenceFactory;
import org.sakaiproject.commons.api.QueryBean;
import org.sakaiproject.commons.api.SakaiProxy;
import org.sakaiproject.commons.api.datamodel.Comment;
import org.sakaiproject.commons.api.datamodel.Post;
import org.sakaiproject.commons.api.datamodel.PostLike;
import org.sakaiproject.commons.api.datamodel.PostsData;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.email.api.EmailService;
import org.sakaiproject.entitybroker.EntityReference;
import org.sakaiproject.entitybroker.EntityView;
import org.sakaiproject.entitybroker.entityprovider.annotations.EntityCustomAction;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ActionsExecutable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.AutoRegisterEntityProvider;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Describeable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.Outputable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.ReferenceParseable;
import org.sakaiproject.entitybroker.entityprovider.capabilities.RequestAware;
import org.sakaiproject.entitybroker.entityprovider.extension.ActionReturn;
import org.sakaiproject.entitybroker.entityprovider.extension.Formats;
import org.sakaiproject.entitybroker.entityprovider.extension.RequestGetter;
import org.sakaiproject.entitybroker.exception.EntityException;
import org.sakaiproject.entitybroker.util.AbstractEntityProvider;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Adrian Fish (adrian.r.fish@gmail.com)
 */
@Setter @Slf4j
public class CommonsEntityProvider extends AbstractEntityProvider implements RequestAware, AutoRegisterEntityProvider, Outputable, Describeable, ActionsExecutable, ReferenceParseable {

    public final static String ENTITY_PREFIX = "commons";

    private final static String USER_AGENT
        = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";

    private CommonsManager commonsManager;
    private CommonsSecurityManager commonsSecurityManager;
    private RequestGetter requestGetter;
    private SakaiProxy sakaiProxy;
    private EmailService emailService;
    private SiteService siteService;
    private UserDirectoryService userDirectoryService;
    private ServerConfigurationService serverConfigurationService;

    private static final ResourceLoader rl = new ResourceLoader("commons");

    private final static List<String> contentTypes
        = Arrays.asList("image/png", "image/jpg", "image/jpeg", "image/gif");

    public Object getSampleEntity() {
        return new Post();
    }

    public String getEntityPrefix() {
        return ENTITY_PREFIX;
    }

    public EntityReference getParsedExemplar() {
       return new EntityReference("sakai:commons");
    }

    public String[] getHandledOutputFormats() {
        return new String[] { Formats.JSON,Formats.XML };
    }

    @EntityCustomAction(action = "posts", viewKey = EntityView.VIEW_LIST)
    public ActionReturn getPosts(EntityView view, Map<String, Object> params) {

        String userId = getCheckedUser();

        String commonsId = view.getPathSegment(2);
        String siteId = (String) params.get("siteId");
        String embedder = (String) params.get("embedder");

        if (StringUtils.isBlank(commonsId) || StringUtils.isBlank(siteId) || StringUtils.isBlank(embedder)) {
            throw new EntityException(
                "Bad request: To get the posts in a commons you need a url like '/direct/commons/posts/COMMONSID.json?siteId=siteId&embedder=SITE'"
                                            , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        List<Post> posts;

        boolean isUserSite = sakaiProxy.isUserSite(siteId);

        QueryBean query = QueryBean.builder()
            .commonsId(commonsId)
            .siteId(siteId)
            .embedder(embedder)
            .userSite(isUserSite)
            .callerId(userId).build();

        try {
            posts = commonsManager.getPosts(query);

            PostsData data = new PostsData();
            data.postsTotal = posts.size();

            int page = Integer.parseInt((String) params.get("page"));

            if (page == -1) {
                // This is a hack to support the multi tool pages. Infinite scroll does not work well
                // in a frame.
                data.status = "END";
                if (posts.size() > 200) {
                    posts = posts.subList(0, 200);
                }
                data.posts = posts;
            } else {
                int pageSize = 20;
                int start  = page * pageSize;

                if (start >= data.postsTotal) {
                    data.status = "END";
                } else {
                    int end = start + pageSize;

                    log.debug("end: {}", end);

                    if (end >= data.postsTotal) {
                        end = data.postsTotal;
                        data.status = "END";
                    }

                    data.posts = posts.subList(start, end);
                }
            }

            // Expensive to fetch this via browser for each post
            for (Post post : data.posts) {
                post.setNumberOfLikes(commonsManager.countPostLikes(post.getId()));
            }

            return new ActionReturn(data);
        } catch (Exception e) {
            log.error("Caught exception whilst getting posts.", e);
            throw new EntityException("Failed to retrieve posts for site " + siteId, "", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @EntityCustomAction(action = "post", viewKey = EntityView.VIEW_LIST)
    public ActionReturn getPost(EntityView view, Map<String, Object> params) {

        String userId = getCheckedUser();

        String postId = (String) params.get("postId");

        if (StringUtils.isBlank(postId)) {
            throw new EntityException("You must supply a postId" , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        Optional<Post> opPost = commonsManager.getPost(postId, true);

        if (opPost.isPresent()) {
            Post post = opPost.get();
            post.setNumberOfLikes(commonsManager.countPostLikes(post.getId()));
            return new ActionReturn(post);
        } else {
            throw new EntityException("No post with id '" + postId + "'" , "", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @EntityCustomAction(action = "savePost", viewKey = EntityView.VIEW_NEW)
    public ActionReturn handleSavePost(Map<String, Object> params) {

        log.debug("handleSavePost");

        String userId = getCheckedUser();

        String content = (String) params.get("content");
        String siteId = (String) params.get("siteId");
        String commonsId = (String) params.get("commonsId");
        String embedder = (String) params.get("embedder");
        boolean priority = Boolean.valueOf((String) params.get("priority"));

        if (StringUtils.isBlank(content) || StringUtils.isBlank(siteId)
                || StringUtils.isBlank(commonsId) || StringUtils.isBlank(embedder)) {
            throw new EntityException("You must supply a siteId, commonsId, embedder and some content"
                                                , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        String id = (String) params.get("id");

        boolean isNew = "".equals(id);

        Post post = new Post();

        if (!isNew) {
            post = commonsManager.getPost(id, false).get();
            post.setContent(content);
        } else {
            post.setCreatorId(userId);
            post.setSiteId(siteId);
            post.setCommonsId(commonsId);
            post.setEmbedder(embedder);
            post.setContent(content);
            post.setPriority(priority);
        }

        Post createdOrUpdatedPost = commonsManager.savePost(post);
        if (createdOrUpdatedPost != null) {
            if (isNew) {
                sakaiProxy.postEvent(CommonsEvents.POST_CREATED,
                                        createdOrUpdatedPost.getReference(),
                                        createdOrUpdatedPost.getSiteId());
                if(priority){   //send an email for a new priority post.
                    sendPriorityEmail(siteId, content);
                }
            } else {
                sakaiProxy.postEvent(CommonsEvents.POST_UPDATED,
                                        createdOrUpdatedPost.getReference(),
                                        createdOrUpdatedPost.getSiteId());
            }
            return new ActionReturn(createdOrUpdatedPost);
        } else {
            return new ActionReturn("FAIL");
        }
    }

    @EntityCustomAction(action = "deletePost", viewKey = EntityView.VIEW_LIST)
    public ActionReturn handleDeletePost(Map<String, Object> params) {

        log.debug("handleDeletePost");

        getCheckedUser();

        String postId = (String) params.get("postId");
        String commonsId = (String) params.get("commonsId");
        String siteId = (String) params.get("siteId");
        if (StringUtils.isBlank(postId) || StringUtils.isBlank(commonsId) || StringUtils.isBlank(siteId)) {
            throw new EntityException("You must supply a postId, a commonsId and a siteId."
                                                , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (commonsManager.deletePost(postId)) {
            String reference = PostReferenceFactory.getReference(commonsId, postId);
            sakaiProxy.postEvent(CommonsEvents.POST_DELETED,
                                    reference,
                                    siteId);
            return new ActionReturn("SUCCESS");
        } else {
            return new ActionReturn("FAIL");
        }
    }

    @EntityCustomAction(action = "likePost", viewKey = EntityView.VIEW_NEW)
    public ActionReturn handleLikePost(Map<String, Object> params){
        String userId = getCheckedUser();
        String postId = (String) params.get("postId");
        commonsManager.likePost(postId, userId);
        return new ActionReturn("SUCCESS");
    }

    @EntityCustomAction(action = "countLikes", viewKey = EntityView.VIEW_LIST)
    public int countPostLikes(Map<String, Object> params){
        String postId = (String) params.get("postId");
        return commonsManager.countPostLikes(postId);
    }

    @EntityCustomAction(action = "doesLike", viewKey = EntityView.VIEW_LIST)
    public int doesUserLike(Map<String, Object> params){
        String userId = getCheckedUser();
        String postId = (String) params.get("postId");
        return commonsManager.doesUserLike(postId, userId);
    }

    @EntityCustomAction(action = "userLikes", viewKey = EntityView.VIEW_LIST)
    public List<PostLike> getAllUserLikes(){
        String userId = getCheckedUser();
        List<PostLike> allLikes = commonsManager.getAllUserLikes(userId);
        return allLikes;
    }

    @EntityCustomAction(action = "postLikes", viewKey = EntityView.VIEW_LIST)
    public List<String> getAllPostLikes(Map<String, Object> params){
        String postId = (String) params.get("postId");
        List<PostLike> allLikes = commonsManager.getAllPostLikes(postId);
        List<String> names = new ArrayList<>();
        for(PostLike like: allLikes){
            names.add(sakaiProxy.getDisplayNameForTheUser(like.getUserId()));
        }
        return names;
    }

    @EntityCustomAction(action = "deleteComment", viewKey = EntityView.VIEW_LIST)
    public ActionReturn handleDeleteComment(Map<String, Object> params) {

        log.debug("handleDeleteComment");

        getCheckedUser();

        String siteId = (String) params.get("siteId");
        String commonsId = (String) params.get("commonsId");
        String postId = (String) params.get("postId");
        String embedder = (String) params.get("embedder");
        String commentId = (String) params.get("commentId");
        String commentCreatorId = (String) params.get("commentCreatorId");
        String postCreatorId = (String) params.get("postCreatorId");

        if (StringUtils.isBlank(siteId) || StringUtils.isBlank(commonsId) || StringUtils.isBlank(postId)
                || StringUtils.isBlank(embedder) || StringUtils.isBlank(commentId)
                || StringUtils.isBlank(commentCreatorId) || StringUtils.isBlank(postCreatorId)) {
            throw new EntityException("You must supply siteId, commonsId, postId, embedder, commentId, commentCreatorId and postCreatorId"
                                                , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        if (commonsManager.deleteComment(siteId, commonsId, embedder, commentId, commentCreatorId, postCreatorId)) {
            String reference = CommonsManager.REFERENCE_ROOT + "/" + commonsId + "/posts/" + postId + "/comments/" + commentId;
            sakaiProxy.postEvent(CommonsEvents.COMMENT_DELETED, reference, siteId);
            return new ActionReturn("SUCCESS");
        } else {
            return new ActionReturn("FAIL");
        }
    }

    @EntityCustomAction(action = "saveComment", viewKey = EntityView.VIEW_NEW)
    public ActionReturn handleSaveComment(Map<String, Object> params) {

        log.debug("handleSaveComment");

        String userId = getCheckedUser();

        String postId = (String) params.get("postId");
        String content = (String) params.get("content");
        String commonsId = (String) params.get("commonsId");
        String siteId = (String) params.get("siteId");

        if (StringUtils.isBlank(content) || StringUtils.isBlank(commonsId) || StringUtils.isBlank(postId) || StringUtils.isBlank(siteId)) {
            throw new EntityException("You must supply a commonsId, siteId, postId and some content"
                                                , "", HttpServletResponse.SC_BAD_REQUEST);
        }

        String id = (String) params.get("id");

        Comment comment = new Comment();
        comment.setId(id);
        comment.setPostId(postId);
        comment.setCreatorId(userId);
        comment.setContent(content);

        content = escape(content);

        boolean isNew = StringUtils.isBlank(comment.getId());

        Comment savedComment = commonsManager.saveComment(commonsId, comment);
        if (savedComment != null) {
            String reference = CommonsManager.REFERENCE_ROOT + "/" + commonsId + "/posts/" + postId + "/comments/" + comment.getId();
            if (isNew) {
                sakaiProxy.postEvent(CommonsEvents.COMMENT_CREATED, reference, siteId);
            } else {
                sakaiProxy.postEvent(CommonsEvents.COMMENT_UPDATED, reference, siteId);
            }
            return new ActionReturn(savedComment);
        } else {
            return new ActionReturn("FAIL");
        }
    }

    @EntityCustomAction(action = "userPerms", viewKey = EntityView.VIEW_LIST)
    public Set<String> handleUserPermsGet(EntityView view, Map<String, Object> params) {

        String userId = getCheckedUser();

        String siteId = (String) params.get("siteId");
        String embedder = (String) params.get("embedder");

        if (StringUtils.isBlank(siteId) || StringUtils.isBlank(embedder)) {
            throw new EntityException("No siteId or embedder supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        return sakaiProxy.getSitePermissionsForCurrentUser(siteId, embedder);
    }

    @EntityCustomAction(action = "perms", viewKey = EntityView.VIEW_LIST)
    public Map<String, Set<String>> handlePermsGet(EntityView view, Map<String, Object> params) {

        String userId = getCheckedUser();

        String siteId = (String) params.get("siteId");

        if (StringUtils.isBlank(siteId)) {
            throw new EntityException("No siteId supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        return sakaiProxy.getSitePermissions(siteId);
    }

    @EntityCustomAction(action = "savePermissions", viewKey = EntityView.VIEW_NEW)
    public String handleSavePermissions(EntityView view, Map<String, Object> params) {

        String userId = getCheckedUser();

        String siteId = (String) params.get("siteId");

        if (sakaiProxy.setPermissionsForSite(siteId, params)) {
            return "success";
        } else {
            throw new EntityException("Failed to set perms", "", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @EntityCustomAction(action = "getUrlMarkup", viewKey = EntityView.VIEW_LIST)
    public ActionReturn getUrlMarkup(OutputStream outputStream, EntityView view, Map<String, Object> params) {

        String userId = getCheckedUser();

        String urlString = (String) params.get("url");

        if (StringUtils.isBlank(urlString)) {
            throw new EntityException("No url supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        }

        try {
            CookieHandler.setDefault( new CookieManager(null, CookiePolicy.ACCEPT_ALL));
            URL url = new URL(urlString);
            URLConnection c = url.openConnection();

            if (c instanceof HttpURLConnection) {
                HttpURLConnection conn = (HttpURLConnection) c;
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setInstanceFollowRedirects(false);
                conn.connect();
                String contentEncoding = conn.getContentEncoding();
                String contentType = conn.getContentType();
                int responseCode = conn.getResponseCode();
                log.debug("Response code: {}", responseCode);

                int redirectCounter = 1;
                while ((responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || responseCode == HttpURLConnection.HTTP_SEE_OTHER) && redirectCounter < 20) {
                    String newUri = conn.getHeaderField("Location");
                    log.debug("{}. New URI: {}", responseCode, newUri);
                    String cookies = conn.getHeaderField("Set-Cookie");
                    url = new URL(newUri);
                    c = url.openConnection();
                    conn = (HttpURLConnection) c;
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestProperty("User-Agent", USER_AGENT);
                    conn.setRequestProperty("Cookie", cookies);
                    conn.connect();
                    contentEncoding = conn.getContentEncoding();
                    contentType = conn.getContentType();
                    responseCode = conn.getResponseCode();
                    log.debug("Redirect counter: {}", redirectCounter);
                    log.debug("Response code: {}", responseCode);
                    redirectCounter += 1;
                }

                if (contentType != null
                        && (contentType.startsWith("text/html")
                                || contentType.startsWith("application/xhtml+xml")
                                || contentType.startsWith("application/xml"))) {
                    String mimeType = contentType.split(";")[0].trim();
                    log.debug("mimeType: {}", mimeType);
                    log.debug("encoding: {}", contentEncoding);

                    BufferedReader reader
                        = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    BufferedWriter writer
                        = new BufferedWriter(new OutputStreamWriter(outputStream));

                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        writer.write(line);
                    }

                    return new ActionReturn(contentEncoding, mimeType, outputStream);
                } else {
                    log.debug("Invalid content type {}. Throwing bad request ...", contentType);
                    throw new EntityException("Url content type not supported", "", HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                throw new EntityException("Url content type not supported", "", HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (MalformedURLException mue) {
            throw new EntityException("Invalid url supplied", "", HttpServletResponse.SC_BAD_REQUEST);
        } catch (IOException ioe) {
            throw new EntityException("Failed to download url contents", "", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @EntityCustomAction(action = "uploadImage", viewKey = EntityView.VIEW_NEW)
    public String uploadImage(EntityView view, Map<String, Object> params) {

        String userId = getCheckedUser();

        String siteId = (String) params.get("siteId");
        FileItem fileItem = (FileItem) params.get("imageFile");
        String contentType = fileItem.getContentType();
        if (!contentTypes.contains(contentType)) {
            throw new EntityException("Invalid image type supplied.", "", HttpServletResponse.SC_BAD_REQUEST);
        }
        String url = sakaiProxy.storeFile(fileItem, siteId);

        if (url == null) {
            throw new EntityException("Failed to save file", "", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return url;
    }

    private String getCheckedUser() throws EntityException {

        String userId = userDirectoryService.getCurrentUser().getId();
        if (userId == null) {
            throw new EntityException("You must be logged in", "", HttpServletResponse.SC_UNAUTHORIZED);
        }
        return userId;
    }

    private String escape(String unescaped) {
        return StringEscapeUtils.escapeJava(unescaped);
    }

    private void sendPriorityEmail(String siteId, String postContent){
        try{
            Set<String> siteUserIds = siteService.getSite(siteId).getUsers();
            Set<User> siteUsers = new HashSet<>();  //will be list of users to email
            for(String id: siteUserIds){
                try{
                    siteUsers.add(userDirectoryService.getUser(id));
                } catch (UserNotDefinedException u){
                    log.error("No user found with ID {} while sending Commons Priority email.", id);
                }
            }
            final String serverName = serverConfigurationService.getServerName();
            final String portalURL = serverConfigurationService.getPortalUrl();
            List<String> headers = new ArrayList<>();
            headers.add("Subject: " + rl.getFormattedMessage("priority_email_subject", siteService.getSite(siteId).getTitle()));
            headers.add("From: " + "\"" + userDirectoryService.getCurrentUser().getDisplayName() + "\" <" + userDirectoryService.getCurrentUser().getEmail() + ">");
            headers.add("Reply-To: " + userDirectoryService.getCurrentUser().getEmail());
            headers.add("Content-Type: text/html");
            StringBuilder messageText = new StringBuilder();    //make message body
            messageText.append(rl.getFormattedMessage("priority_email_added", "<a href=\"" + siteService.getSite(siteId).getUrl() + "\" >" + siteService.getSite(siteId).getTitle() + "</a> "));
            messageText.append("<br><br>" + postContent);
            messageText.append("<br><br><hr><em>");
            messageText.append(rl.getFormattedMessage("priority_email_forwarded", serverName, portalURL, portalURL, "<a href=\"" + siteService.getSite(siteId).getUrl() + "\" >" + siteService.getSite(siteId).getTitle() + "</a> "));
            messageText.append("</em>");
            emailService.sendToUsers(siteUsers, headers, messageText.toString());
        } catch (IdUnusedException e){
            log.error("Cannot send email; no site with ID {}", siteId);
        }
    }
}