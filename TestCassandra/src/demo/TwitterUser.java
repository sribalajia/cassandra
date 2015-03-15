package demo;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Author : asribalaji
 */
public class TwitterUser {
    String id;
    String screenName;
    String userName;
    String location;
    Date createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    static Map<String, String> getUserMap(TwitterUser user) {
        Map<String, String> userMap = new HashMap<String, String>();
        //userMap.put("screenName", user.getScreenName());
        userMap.put("user_name", user.getScreenName());
        userMap.put("location", user.getLocation());
        userMap.put("id", String.valueOf(user.getId()));
        userMap.put("createdAt", String.valueOf(user.getCreatedAt().getTime()));
        return userMap;
    }

    public static TwitterUser convertMaptoUser(Map<String, String> userMap) {
        TwitterUser user = new TwitterUser();
        user.setId(userMap.get("id"));
        user.setScreenName(userMap.get(CassandraUtil.KEY));
        user.setUserName(userMap.get("user_name"));
        Long createdAt = Long.parseLong(userMap.get("createdAt"));
        user.setCreatedAt(new Date(createdAt));
        user.setLocation(userMap.get("location"));
        return user;
    }
    public String toString() {
        return "User {" +
                "id='" + id + '\'' +
                ", screenName='" + screenName + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", userName='" + userName + '\'' +
                ", location='" + location + '\'' +
                '}';
    }
}
