package com.intuit.wasabi.userdirectory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static com.intuit.wasabi.userdirectory.UserDirectoryAnnotations.USER_DIRECTORY_PATH;
import static org.apache.commons.lang3.StringUtils.trimToNull;

public class UserInfoListProvider implements Provider<List<UserInfo>> {
    private final Logger logger = LoggerFactory.getLogger(UserInfoListProvider.class);
    private final List<UserInfo> users = new ArrayList<>();

    @Inject
    public UserInfoListProvider(@Named(USER_DIRECTORY_PATH) String userDirectoryPath) {
        Properties properties = create(userDirectoryPath, UserDirectoryModule.class);
        String userIds = getProperty("user.ids", properties);
        for (String userId : userIds.split(":")) {
            userId = trimToNull(userId);
            // format userId:username:password:email:firstname:lastname
            if (userId != null) {
                String userCredentials = getProperty("user." + userId, properties);

                userCredentials = trimToNull(userCredentials);

                if (userCredentials != null) {
                    final String[] userCredential = userCredentials.split(":", -1);

                    users.add(new UserInfo.Builder(UserInfo.Username.valueOf(userCredential[0]))
                            .withUserId(userId)
                            .withPassword(userCredential[1])
                            .withEmail(userCredential[2])
                            .withFirstName(userCredential[3])
                            .withLastName(userCredential[4])
                            .build());
                }
            }
        }
    }

    @Override
    public List<UserInfo> get() {
        return users;
    }
}