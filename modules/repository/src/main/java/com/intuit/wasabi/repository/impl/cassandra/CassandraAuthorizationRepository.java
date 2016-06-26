/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.impl.cassandra;

import com.google.inject.Inject;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorizationobjects.*;
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.cassandra.ExperimentDriver;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.repository.AuthorizationRepository;
import com.intuit.wasabi.repository.CassandraRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ApplicationNameSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.UsernameSerializer;
import com.intuit.wasabi.userdirectory.UserDirectory;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Cassandra AuthorizationRepository implementation
 *
 * @see AuthorizationRepository
 */
public class CassandraAuthorizationRepository implements AuthorizationRepository {

    private static final Logger LOGGER = getLogger(CassandraAuthorizationRepository.class);
    private static final String USER_NON_NULL_MSG = "Parameter \"userID\" cannot be null";
    private static final String APPLICATION_NON_NULL_MSG = "Parameter \"applicationName\" cannot be null";
    private static final String CQL_ROLES = "select * from user_roles where user_id = ? and app_name = ?";
    private final CassandraDriver driver;
    private final ExperimentsKeyspace keyspace;
    private final ExperimentRepository experimentRepository;
    private final UserDirectory userDirectory;

    /**
     * Constructor
     *
     * @param driver               cassandra driver
     * @param keyspace             cassandra keyspace
     * @param experimentRepository experiment repository
     * @param userDirectory        user tools
     * @throws IOException         io exception
     * @throws ConnectionException connection exception
     */
    @Inject
    public CassandraAuthorizationRepository(
            @ExperimentDriver CassandraDriver driver,
            ExperimentsKeyspace keyspace,
            @CassandraRepository ExperimentRepository experimentRepository,
            UserDirectory userDirectory)
            throws IOException, ConnectionException {
        super();
        this.driver = driver;
        this.keyspace = keyspace;
        this.experimentRepository = experimentRepository;
        this.userDirectory = userDirectory;
    }

    private UserRole getUserRole(UserInfo.Username userID, Application.Name applicationName) {

        checkNotNull(userID, USER_NON_NULL_MSG);
        checkNotNull(applicationName, APPLICATION_NON_NULL_MSG);

        try {
            OperationResult<CqlResult<UserInfo.Username, String>> opResult =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userRolesCF())
                            .withCql(CQL_ROLES)
                            .asPreparedStatement()
                            .withByteBufferValue(userID, UsernameSerializer.get())
                            .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                            .execute();

            Rows<UserInfo.Username, String> rows = opResult.getResult().getRows();

            UserRole userRole = null;
            if (!rows.isEmpty()) {
                assert rows.size() <= 1 : "More than a single row returned";
                Row<UserInfo.Username, String> row = rows.getRowByIndex(0);
                Role role = Role.toRole(row.getColumns().getStringValue("role", ""));
                UserInfo userInfo = getUserInfo(userID);
                if (userInfo == null) {
                    userInfo = lookupUser(userID);
                    setUserInfo(userInfo);
                }

                userRole = UserRole.newInstance(applicationName, role)
                        .withUserID(userID)
                        .withUserEmail(userInfo.getEmail())
                        .withFirstName(userInfo.getFirstName())
                        .withLastName(userInfo.getLastName())
                        .build();
            }

            return userRole;

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve roles for user \"" + userID + "\"", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.intuit.wasabi.repository.AuthorizationRepository#getUserPermissionsList(com.intuit.wasabi.authenticationobjects.UserInfo.Username)
     */
    @Override
    public UserPermissionsList getUserPermissionsList(UserInfo.Username userID) {
        checkNotNull(userID, USER_NON_NULL_MSG);

        try {
            UserPermissionsList userPermissionsList = new UserPermissionsList();
            //check if superadmin privileges exist
            final String cql = "select * from user_roles where user_id = ? and app_name = '*'";

            OperationResult<CqlResult<UserInfo.Username, String>> opResult =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userRolesCF())
                            .withCql(cql)
                            .asPreparedStatement()
                            .withByteBufferValue(userID, UsernameSerializer.get())
                            .execute();
            Rows<UserInfo.Username, String> rows = opResult.getResult().getRows();
            UserPermissions userPermissions;

            if (!rows.isEmpty()) {
                for (Row<UserInfo.Username, String> row : rows) {
                    if ("superadmin".equals(row.getColumns().getStringValue("role", ""))) {
                        //now get a list of ALL applications
                        List<Application.Name> applicationNameList = experimentRepository.getApplicationsList();
                        for (Application.Name applicationName : applicationNameList) {
                            userPermissions = UserPermissions.newInstance(applicationName, Role.SUPERADMIN.getRolePermissions
                                    ()).build();
                            userPermissionsList.addPermissions(userPermissions);
                        }
                        return userPermissionsList;
                    }
                }
            }

            //check for individual app permissions if user is not Super admin
            final String cql1 = "select * from user_roles where user_id = ?";
            OperationResult<CqlResult<UserInfo.Username, String>> opResult1 =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userRolesCF())
                            .withCql(cql1)
                            .asPreparedStatement()
                            .withByteBufferValue(userID, UsernameSerializer.get())
                            .execute();
            Rows<UserInfo.Username, String> rows1 = opResult1.getResult().getRows();

            for (Row<UserInfo.Username, String> row1 : rows1) {
                Role role = Role.toRole(row1.getColumns().getStringValue("role", ""));
                Application.Name applicationName = Application.Name.valueOf(row1.getColumns().getStringValue("app_name",
                        ""));
                assert role != null;
                userPermissions = UserPermissions.newInstance(applicationName, role.getRolePermissions()).build();
                userPermissionsList.addPermissions(userPermissions);
            }

            return userPermissionsList;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve roles for user \"" + userID + "\"", e);
        }
    }

    /**
     * @param applicationName Application.Name Object
     * @return userRoleList UserRoleList
     */
    /*
     * (non-Javadoc)
     * @see com.intuit.wasabi.repository.AuthorizationRepository#getApplicationUsers(com.intuit.wasabi.experimentobjects.Application.Name)
     */
    @Override
    public UserRoleList getApplicationUsers(Application.Name applicationName) {
        checkNotNull(applicationName, APPLICATION_NON_NULL_MSG);

        final String cql = "select * from app_roles where app_name = ?";

        try {
            OperationResult<CqlResult<Application.Name, String>> opResult =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.appRoleCF())
                            .withCql(cql)
                            .asPreparedStatement()
                            .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                            .execute();

            Rows<Application.Name, String> rows = opResult.getResult().getRows();
            UserRoleList userRoleList = new UserRoleList();

            for (Row<Application.Name, String> row : rows) {
                Application.Name appName = Application.Name.valueOf(row.getColumns().getStringValue("app_name", ""));
                Role role = Role.toRole(row.getColumns().getStringValue("role", ""));
                UserInfo.Username userID = UserInfo.Username.valueOf(row.getColumns().getStringValue("user_id", ""));
                UserInfo userInfo = getUserInfo(userID);

                if (userInfo == null) {
                    userInfo = lookupUser(userID);
                }

                UserRole userRole = UserRole.newInstance(appName, role)
                        .withUserID(userID)
                        .withUserEmail(userInfo.getEmail())
                        .withFirstName(userInfo.getFirstName())
                        .withLastName(userInfo.getLastName())
                        .build();
                userRoleList.addRole(userRole);
            }

            return userRoleList;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve roles for application name \"" + applicationName + "\"", e);
        }
    }

    private UserInfo lookupUser(UserInfo.Username userID) {
        UserInfo userInfo;
        try {
            LOGGER.debug("Workforce-getApplicationUsers: looking up user {}", userID.toString());
            userInfo = userDirectory.lookupUser(userID);
        } catch (AuthenticationException e) {
            LOGGER.warn(String.format("Workforce-getApplicationUsers: problem looking up user %s", userID.toString()), e);
            userInfo = UserInfo.newInstance(userID)
                    .withEmail("")
                    .withFirstName("")
                    .withLastName("")
                    .build();
        }
        return userInfo;
    }

    /*
     * (non-Javadoc)
     * @see com.intuit.wasabi.repository.AuthorizationRepository#getUserPermissions(com.intuit.wasabi.authenticationobjects.UserInfo.Username, com.intuit.wasabi.experimentobjects.Application.Name)
     */
    @Override
    public UserPermissions getUserPermissions(UserInfo.Username userID, Application.Name applicationName) {
        checkNotNull(userID, USER_NON_NULL_MSG);
        checkNotNull(applicationName, APPLICATION_NON_NULL_MSG);

        try {
            //check if superadmin privileges exist
            final String cql = "select * from user_roles where user_id = ? and app_name = '*'";
            OperationResult<CqlResult<UserInfo.Username, String>> opResult =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userRolesCF())
                            .withCql(cql)
                            .asPreparedStatement()
                            .withByteBufferValue(userID, UsernameSerializer.get())
                            .execute();
            Rows<UserInfo.Username, String> rows = opResult.getResult().getRows();
            UserPermissions userPermissions = null;

            if (!rows.isEmpty()) {
                for (Row<UserInfo.Username, String> row : rows) {
                    if ("superadmin".equals(row.getColumns().getStringValue("role", ""))) {
                        userPermissions = UserPermissions.newInstance(applicationName, Role.SUPERADMIN.getRolePermissions())
                                .build();
                        return userPermissions;
                    }
                }
            }

            //see if app-specific permissions exist
            OperationResult<CqlResult<UserInfo.Username, String>> opResult1 = driver.getKeyspace()
                    .prepareQuery(keyspace.userRolesCF())
                    .withCql(CQL_ROLES)
                    .asPreparedStatement()
                    .withByteBufferValue(userID, UsernameSerializer.get())
                    .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                    .execute();

            Rows<UserInfo.Username, String> rows1 = opResult1.getResult().getRows();
            if (!rows1.isEmpty()) {
                assert rows1.size() <= 1 : "More than a single row returned";

                Row<UserInfo.Username, String> row1 = rows1.getRowByIndex(0);
                Role role = Role.toRole(row1.getColumns().getStringValue("role", ""));
                assert role != null;
                userPermissions = UserPermissions.newInstance(applicationName, role.getRolePermissions()).build();
            }

            return userPermissions;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve permissions for user \"" + userID + "\" and application " +
                    "\"" + applicationName + "\"", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.intuit.wasabi.repository.AuthorizationRepository#deleteUserRole(com.intuit.wasabi.authenticationobjects.UserInfo.Username, com.intuit.wasabi.experimentobjects.Application.Name)
     */
    @Override
    public void deleteUserRole(UserInfo.Username userID, Application.Name applicationName) {

        //delete the record from the first table
        String cql = "delete from app_roles where app_name = ? and user_id = ?";
        try {
            driver.getKeyspace().prepareQuery(keyspace.appRoleCF())
                    .withCql(cql)
                    .asPreparedStatement()
                    .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                    .withByteBufferValue(userID, UsernameSerializer.get())
                    .execute();

        } catch (Exception e) {
            throw new RepositoryException("Could not delete role for user " + userID +
                    "in application \"" + applicationName + "\"", e);
        }

        //delete the record from the second table
        UserRole userRole = getUserRole(userID, applicationName);
        cql = "delete from user_roles where user_id = ? and app_name = ?";
        try {
            driver.getKeyspace().prepareQuery(keyspace.appRoleCF())
                    .withCql(cql)
                    .asPreparedStatement()
                    .withByteBufferValue(userID, UsernameSerializer.get())
                    .withByteBufferValue(applicationName, ApplicationNameSerializer.get())
                    .execute();

        } catch (Exception e) {
            //if there was an exception, put the data back in the first table
            setUserRole(userRole);
            throw new RepositoryException("Could not delete role for user " + userID +
                    "in application \"" + applicationName + "\"", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see com.intuit.wasabi.repository.AuthorizationRepository#setUserRole(com.intuit.wasabi.authorizationobjects.UserRole)
     */
    @Override
    public void setUserRole(UserRole userRole) {

        checkNotNull(userRole, "Parameter \"userRole\" cannot be null");
        String cql = "insert into user_roles (user_id, app_name, role) values (?, ?, ?)";
        try {
            driver.getKeyspace()
                    .prepareQuery(keyspace.userRolesCF())
                    .withCql(cql)
                    .asPreparedStatement()
                    .withByteBufferValue(userRole.getUserID(), UsernameSerializer.get())
                    .withByteBufferValue(userRole.getApplicationName(), ApplicationNameSerializer.get())
                    .withStringValue(userRole.getRole().toString())
                    .execute();

        } catch (Exception e) {
            throw new RepositoryException("Could not set roles for user \"" + userRole.getUserID() + "\"", e);
        }

        cql = "insert into app_roles (app_name, user_id, role) values (?, ?, ?)";
        try {
            driver.getKeyspace()
                    .prepareQuery(keyspace.userRolesCF())
                    .withCql(cql)
                    .asPreparedStatement()
                    .withByteBufferValue(userRole.getApplicationName(), ApplicationNameSerializer.get())
                    .withByteBufferValue(userRole.getUserID(), UsernameSerializer.get())
                    .withStringValue(userRole.getRole().toString())
                    .execute();

        } catch (Exception e) {
            //delete the record from the first table
            cql = "delete from user_roles where user_id = ? and app_name = ?";
            try {
                driver.getKeyspace().prepareQuery(keyspace.appRoleCF())
                        .withCql(cql)
                        .asPreparedStatement()
                        .withByteBufferValue(userRole.getApplicationName(), ApplicationNameSerializer.get())
                        .withStringValue(userRole.getRole().toString())
                        .execute();

            } catch (Exception e2) {
                throw new RepositoryException("Could not rollback role for user " + userRole.getRole().toString() +
                        "in application \"" + userRole.getApplicationName() + "\"", e2);
            }
            throw new RepositoryException("Could not set roles for user \"" + userRole.getUserID() + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserRoleList getUserRoleList(UserInfo.Username userID) {

        checkNotNull(userID, USER_NON_NULL_MSG);

        try {
            UserRoleList userRoleList = new UserRoleList();
            //check if superadmin privileges exist
            final String cql = "select * from user_roles where user_id = ? and app_name = '*'";
            OperationResult<CqlResult<UserInfo.Username, String>> opResult =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userRolesCF())
                            .withCql(cql)
                            .asPreparedStatement()
                            .withByteBufferValue(userID, UsernameSerializer.get())
                            .execute();
            Rows<UserInfo.Username, String> rows = opResult.getResult().getRows();
            UserRole userRole;
            //get user info from cassandra
            UserInfo userInfo = getUserInfo(userID);

            //if the user did not exist in cassandra
            if (userInfo == null) {
                try {
                    //get the user from workforce lookup
                    LOGGER.debug("Workforce-getUserRoleList: looking up user " + userID.toString());
                    userInfo = userDirectory.lookupUser(userID);
                } catch (AuthenticationException e) {
                    //if workforce lookup fails, throw an exception
                    LOGGER.warn("Workforce-getUserRoleList: problem looking up user" + userID.toString(), e);
                    userInfo = UserInfo.newInstance(userID)
                            .withEmail("")
                            .withFirstName("")
                            .withLastName("")
                            .build();
                }
                //if workforce lookup succeeds, then save it in cassandra
                setUserInfo(userInfo);
            }

            if (!rows.isEmpty()) {
                for (Row<UserInfo.Username, String> row : rows) {
                    if ("superadmin".equals(row.getColumns().getStringValue("role", ""))) {
                        //now get a list of ALL applications
                        List<Application.Name> applicationNameList = experimentRepository.getApplicationsList();
                        for (Application.Name applicationName : applicationNameList) {
                            userRole = UserRole.newInstance(applicationName, Role.SUPERADMIN)
                                    .withUserID(userID)
                                    .withUserEmail(userInfo.getEmail())
                                    .withFirstName(userInfo.getFirstName())
                                    .withLastName(userInfo.getLastName())
                                    .build();
                            userRoleList.addRole(userRole);
                        }
                        return userRoleList;
                    }
                }
            }

            //check for individual app permissions if user is not Super admin
            final String cql1 = "select * from user_roles where user_id = ?";
            OperationResult<CqlResult<UserInfo.Username, String>> opResult1 =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userRolesCF())
                            .withCql(cql1)
                            .asPreparedStatement()
                            .withByteBufferValue(userID, UsernameSerializer.get())
                            .execute();
            Rows<UserInfo.Username, String> rows1 = opResult1.getResult().getRows();

            for (Row<UserInfo.Username, String> row1 : rows1) {
                Role role = Role.toRole(row1.getColumns().getStringValue("role", ""));
                Application.Name applicationName = Application.Name.valueOf(row1.getColumns().getStringValue("app_name",
                        ""));
                userRole = UserRole.newInstance(applicationName, role)
                        .withUserID(userID)
                        .withUserEmail(userInfo.getEmail())
                        .withFirstName(userInfo.getFirstName())
                        .withLastName(userInfo.getLastName())
                        .build();
                userRoleList.addRole(userRole);
            }

            return userRoleList;
        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve roles for user \"" + userID + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserInfo getUserInfo(UserInfo.Username userID) {

        try {
            String cql = "select * from user_info where user_id = ?";
            OperationResult<CqlResult<UserInfo.Username, String>> opResult =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userInfoCF())
                            .withCql(cql)
                            .asPreparedStatement()
                            .withByteBufferValue(userID, UsernameSerializer.get())
                            .execute();

            Rows<UserInfo.Username, String> rows = opResult.getResult().getRows();

            if (rows.isEmpty()) {
                return null;
            } else if (rows.size() > 1) {
                throw new AuthenticationException("error, more than one user with the userID " + userID.toString());
            }

            return UserInfo.newInstance(userID)
                    .withEmail(rows.getRowByIndex(0).getColumns().getStringValue("user_email", ""))
                    .withFirstName(rows.getRowByIndex(0).getColumns().getStringValue("firstname", ""))
                    .withLastName(rows.getRowByIndex(0).getColumns().getStringValue("lastname", ""))
                    .build();

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve info for user \"" + userID + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUserInfo(UserInfo userInfo) {

        try {

            String cql = "insert into user_info (user_id, user_email, firstname, lastname) values (?, ?, ?, ?)";

            driver.getKeyspace()
                    .prepareQuery(keyspace.userInfoCF())
                    .withCql(cql)
                    .asPreparedStatement()
                    .withByteBufferValue(userInfo.getUsername(), UsernameSerializer.get())
                    .withStringValue(userInfo.getEmail())
                    .withStringValue(userInfo.getFirstName())
                    .withStringValue(userInfo.getLastName())
                    .execute();

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not set info for user \"" + userInfo.getUsername() + "\"", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserPermissions checkSuperAdminPermissions(UserInfo.Username userID, Application.Name applicationName) {
        String cql = "select * from user_roles where user_id = ? and app_name = '*'";
        OperationResult<CqlResult<UserInfo.Username, String>> opResult;

        try {
            opResult =
                    driver.getKeyspace()
                            .prepareQuery(keyspace.userRolesCF())
                            .withCql(cql)
                            .asPreparedStatement()
                            .withByteBufferValue(userID, UsernameSerializer.get())
                            .execute();

        } catch (ConnectionException e) {
            throw new RepositoryException("Could not retrieve permissions for user \"" + userID + "\" and application " +
                    "\"" + applicationName + "\"", e);
        }

        Rows<UserInfo.Username, String> rows = opResult.getResult().getRows();

        UserPermissions userPermissions;
        if (!rows.isEmpty()) {
            for (Row<UserInfo.Username, String> row : rows) {
                if ("superadmin".equals(row.getColumns().getStringValue("role", ""))) {
                    userPermissions = UserPermissions.newInstance(applicationName, Role.SUPERADMIN.getRolePermissions())
                            .build();
                    return userPermissions;
                }
            }
        }
        return null;
    }

}
