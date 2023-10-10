package fr.lesprojetscagnottes.core.providers.slack.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.lesprojetscagnottes.core.common.service.HttpClientService;
import fr.lesprojetscagnottes.core.common.strings.StringsCommon;
import fr.lesprojetscagnottes.core.notification.entity.NotificationEntity;
import fr.lesprojetscagnottes.core.notification.model.NotificationVariables;
import fr.lesprojetscagnottes.core.providers.slack.entity.SlackNotificationEntity;
import fr.lesprojetscagnottes.core.providers.slack.entity.SlackTeamEntity;
import fr.lesprojetscagnottes.core.providers.slack.entity.SlackUserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SlackClientService {

    @Value("${fr.lesprojetscagnottes.slack.client_id}")
    private String slackClientId;

    @Value("${fr.lesprojetscagnottes.slack.client_secret}")
    private String slackClientSecret;

    private final ResourceLoader resourceLoader;
    private final SpringTemplateEngine templateEngine;
    private final Gson gson;
    private final HttpClientService httpClientService;

    @Autowired
    public SlackClientService(ResourceLoader resourceLoader, SpringTemplateEngine templateEngine, Gson gson, HttpClientService httpClientService) {
        this.resourceLoader = resourceLoader;
        this.templateEngine = templateEngine;
        this.gson = gson;
        this.httpClientService = httpClientService;
    }

    public SlackUserEntity token(String code, String redirect_uri) {
        String url = "https://slack.com/api/oauth.v2.access?client_id=" + slackClientId + "&client_secret=" + slackClientSecret + "&code=" + code + "&redirect_uri=" + redirect_uri;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json; charset=utf-8")
                .GET()
                .build();

        SlackUserEntity slackUser = null;

        try {
            log.debug("Call {}", url);
            HttpResponse<String> response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Response from {} : {}", url, response.body());
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            log.debug("Response converted into json : {}", json);
            log.debug("authed_user : {}", json.get("authed_user"));
            log.debug("team : {}", json.get("team"));
            if (json.get("authed_user") != null && json.get("team") != null) {
                JsonObject jsonUser = json.get("authed_user").getAsJsonObject();
                slackUser = new SlackUserEntity();
                slackUser.setAccessToken(jsonUser.get("access_token").getAsString());
                slackUser.setSlackId(jsonUser.get("id").getAsString());
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return slackUser;
    }

    public SlackTeamEntity getTeam(String token) {
        String url = "https://slack.com/api/team.info";
        log.debug("GET " + url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> response;
        SlackTeamEntity slackTeam = null;
        try {
            response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("response : " + response.body());
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if (json.get("ok") != null && json.get("ok").getAsBoolean()) {
                JsonObject jsonTeam = json.get("team").getAsJsonObject();
                slackTeam = new SlackTeamEntity();
                slackTeam.setTeamId(jsonTeam.get("id").getAsString());
                slackTeam.setTeamName(jsonTeam.get("name").getAsString());
                slackTeam.setImage_132(jsonTeam.getAsJsonObject("icon").get("image_132").getAsString());
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return slackTeam;
    }

    public SlackUserEntity getUser(String token, String user) {
        String url = "https://slack.com/api/users.info?user=" + user;
        log.debug("GET " + url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> response;
        SlackUserEntity slackUser = null;
        try {
            response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("response : " + response.body());
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if (json.get("ok") != null && json.get("ok").getAsBoolean()) {
                JsonObject jsonUser = json.get("user").getAsJsonObject().get("profile").getAsJsonObject();
                slackUser = new SlackUserEntity();
                slackUser.setSlackId(user);
                slackUser.setName(jsonUser.get("real_name").getAsString());
                slackUser.setEmail(jsonUser.get("email").getAsString());
                slackUser.setImage_192(jsonUser.get("image_192").getAsString());
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return slackUser;
    }

    public void postMessage(SlackTeamEntity slackTeam, String channelId, String text) {
        String url = "https://slack.com/api/chat.postMessage";
        String body = "{\"channel\":\"" + channelId + "\", \"text\":\"" + text.replaceAll("(\\r\\n|\\n)", "\\\\n") + "\"}";
        log.debug("POST " + url);
        log.debug("body : " + body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + slackTeam.getBotAccessToken())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("response : " + response.body());
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void postMessage(SlackTeamEntity slackTeam, String channelId, String text, String blocks) {
        String url = "https://slack.com/api/chat.postMessage";
        String body = "{\"channel\":\"" + channelId +
                "\", \"text\":\"" + text.replaceAll("(\\r\\n|\\n)", "\\\\n") +
                "\", \"blocks\":[" + blocks.replaceAll("(\\r\\n|\\n)", "") + "]}";
        log.debug("POST " + url);
        log.debug("body : " + body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + slackTeam.getBotAccessToken())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("response : " + response.body());
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void inviteBotInConversation(SlackTeamEntity slackTeam) {
        String url = "https://slack.com/api/conversations.invite";
        String body = "{\"channel\":\"" + slackTeam.getPublicationChannelId() + "\", \"users\": \"" + slackTeam.getBotUserId() + "\"}";
        log.debug("POST " + url);
        log.debug("body : " + body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + slackTeam.getAccessToken())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("response : " + response.body());
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    public List<SlackUserEntity> listUsers(SlackTeamEntity slackTeam) {
        String url = "https://slack.com/api/users.list";
        String body = "{}";
        log.debug("POST {}", url);
        log.debug("body : {}", body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + slackTeam.getBotAccessToken())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response;
        List<SlackUserEntity> slackUsers = new ArrayList<>();
        try {
            response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("response : " + response.body());
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if (json.get("ok") != null && json.get("ok").getAsBoolean()) {

                JsonArray membersJsonArray = json.get("members").getAsJsonArray();
                membersJsonArray.forEach(memberJsonElement -> {
                    JsonObject memberJson = memberJsonElement.getAsJsonObject();

                    SlackUserEntity slackUser = new SlackUserEntity();
                    slackUser.setSlackId(memberJson.get("id").getAsString());
                    slackUser.setDeleted(memberJson.get("deleted").getAsBoolean());

                    JsonElement isRestricted = memberJson.get("is_restricted");
                    if (isRestricted != null)
                        slackUser.setIsRestricted(isRestricted.getAsBoolean());

                    memberJson = memberJson.get("profile").getAsJsonObject();
                    if (memberJson.get("email") != null) {
                        slackUser.setEmail(memberJson.get("email").getAsString());
                        slackUser.setName(memberJson.get("real_name").getAsString());
                        slackUser.setImage_192(memberJson.get("image_192").getAsString());

                        slackUsers.add(slackUser);
                    }
                });
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return slackUsers;
    }

    public String openDirectMessageChannel(SlackTeamEntity slackTeam, String slackUserId) {
        String url = "https://slack.com/api/conversations.open";
        String body = "{\"users\": \"" + slackUserId + "\"}";
        log.debug("POST " + url);
        log.debug("body : " + body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + slackTeam.getBotAccessToken())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("response : " + response.body());
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if (json.get("ok") != null && json.get("ok").getAsBoolean()) {
                return json.get("channel").getAsJsonObject().get("id").getAsString();
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return StringsCommon.EMPTY_STRING;
    }

    public String getBotId(SlackTeamEntity slackTeam) {
        String url = "https://slack.com/api/auth.test";
        log.debug("GET " + url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + slackTeam.getBotAccessToken())
                .build();
        HttpResponse<String> response;
        String botId = null;
        try {
            response = httpClientService.getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("response : " + response.body());
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            if (json.get("ok") != null && json.get("ok").getAsBoolean() && !json.get("bot_id").isJsonNull()) {
                botId = json.get("bot_id").getAsString();
            }
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        return botId;
    }

    public void sendNotification(NotificationEntity notification, SlackNotificationEntity slackNotification) {
        Context context = new Context();
        context.setVariables(gson.fromJson(notification.getVariables(), NotificationVariables.class));

        String templatePath = "slack/fr/" + notification.getName();
        String blocksPath = "templates/" + templatePath + ".blocks";

        String message = templateEngine.process(templatePath, context);

        String blocks = null;
        try {
            Resource resource = resourceLoader.getResource("classpath:" + blocksPath + ".txt");
            log.debug("Verify existence of {} : {}", resource.getFilename(), resource.exists());
            if(resource.exists()) {
                blocks = templateEngine.process(templatePath + ".blocks", context);
            }
        } catch (Exception e) {
            log.debug("File {} does not exist in classpath", blocksPath, e);
        }

        log.debug("Sending notification Slack {} with blocks {}", message, blocks);

        SlackTeamEntity slackTeam = slackNotification.getTeam();
        inviteBotInConversation(slackTeam);
        if(blocks != null) {
            postMessage(slackTeam, slackTeam.getPublicationChannelId(), message, blocks);
        } else {
            postMessage(slackTeam, slackTeam.getPublicationChannelId(), message);
        }
    }
}
