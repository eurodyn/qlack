package com.eurodyn.qlack.fuse.aaa.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.eurodyn.qlack.fuse.aaa.dto.GroupHasOperationDTO;
import com.eurodyn.qlack.fuse.aaa.dto.OperationDTO;
import com.eurodyn.qlack.fuse.aaa.dto.ResourceDTO;
import com.eurodyn.qlack.fuse.aaa.exception.DynamicOperationException;
import com.eurodyn.qlack.fuse.aaa.mappers.GroupHasOperationMapper;
import com.eurodyn.qlack.fuse.aaa.mappers.OperationMapper;
import com.eurodyn.qlack.fuse.aaa.mappers.ResourceMapper;
import com.eurodyn.qlack.fuse.aaa.model.Group;
import com.eurodyn.qlack.fuse.aaa.model.GroupHasOperation;
import com.eurodyn.qlack.fuse.aaa.model.OpTemplate;
import com.eurodyn.qlack.fuse.aaa.model.OpTemplateHasOperation;
import com.eurodyn.qlack.fuse.aaa.model.Operation;
import com.eurodyn.qlack.fuse.aaa.model.Resource;
import com.eurodyn.qlack.fuse.aaa.model.User;
import com.eurodyn.qlack.fuse.aaa.model.UserHasOperation;
import com.eurodyn.qlack.fuse.aaa.repository.GroupHasOperationRepository;
import com.eurodyn.qlack.fuse.aaa.repository.GroupRepository;
import com.eurodyn.qlack.fuse.aaa.repository.OpTemplateRepository;
import com.eurodyn.qlack.fuse.aaa.repository.OperationRepository;
import com.eurodyn.qlack.fuse.aaa.repository.ResourceRepository;
import com.eurodyn.qlack.fuse.aaa.repository.UserHasOperationRepository;
import com.eurodyn.qlack.fuse.aaa.repository.UserRepository;
import com.eurodyn.qlack.fuse.aaa.service.OperationService;

import bsh.EvalError;
import bsh.Interpreter;

/**
 * @author European Dynamics SA
 */
@Service
@Validated
@Transactional
public class OperationServiceImpl implements OperationService {

    private static final Logger LOGGER = Logger.getLogger(OperationServiceImpl.class.getName());

    //Repositories
    private final OperationRepository operationRepository;
    private final UserHasOperationRepository userHasOperationRepository;
    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final OpTemplateRepository opTemplateRepository;
    private final GroupHasOperationRepository groupHasOperationRepository;
    private final GroupRepository groupRepository;
    //Mappers
    private final OperationMapper operationMapper;
    private final ResourceMapper resourceMapper;
    private final GroupHasOperationMapper groupHasOperationMapper;

    public OperationServiceImpl(
        OperationRepository operationRepository,
        UserHasOperationRepository userHasOperationRepository, UserRepository userRepository,
        ResourceRepository resourceRepository, OpTemplateRepository opTemplateRepository,
        GroupHasOperationRepository groupHasOperationRepository,
        GroupRepository groupRepository, OperationMapper operationMapper,
        ResourceMapper resourceMapper, GroupHasOperationMapper groupHasOperationMapper) {
        this.operationRepository = operationRepository;
        this.userHasOperationRepository = userHasOperationRepository;
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.opTemplateRepository = opTemplateRepository;
        this.groupHasOperationRepository = groupHasOperationRepository;
        this.groupRepository = groupRepository;
        this.operationMapper = operationMapper;
        this.resourceMapper = resourceMapper;
        this.groupHasOperationMapper = groupHasOperationMapper;
    }

    private boolean prioritisePositive;

    public void setPrioritisePositive(boolean prioritisePositive) {
        this.prioritisePositive = prioritisePositive;
    }

    public String createOperation(OperationDTO operationDTO) {
        Operation operation = operationMapper.mapToEntity(operationDTO);
        operationRepository.save(operation);

        return operation.getId();
    }

    public void updateOperation(OperationDTO operationDTO) {
        Operation operation = operationRepository.fetchById(operationDTO.getId());
        operationMapper.mapToExistingEntity(operationDTO, operation);
    }

    public void deleteOperation(String operationID) {

        operationRepository.delete(findById(operationID));
    }

    private Operation findById(String operationId) {

        return operationRepository.fetchById(operationId);
    }

    public List<OperationDTO> getAllOperations() {
        return operationMapper.mapToDTO(operationRepository.findAll());
    }

    public OperationDTO getOperationByName(String operationName) {
        return operationMapper.mapToDTO(operationRepository.findByName(operationName));
    }

    public void addOperationToUser(String userID, String operationName, boolean isDeny) {
        UserHasOperation uho = userHasOperationRepository.findByUserIdAndOperationName(userID, operationName);
        if (uho != null) {
            uho.setDeny(isDeny);
        } else {
            User user = userRepository.fetchById(userID);
            Operation operation = operationRepository.findByName(operationName);
            uho = new UserHasOperation();
            uho.setDeny(isDeny);
            user.addUserHasOperation(uho);
            operation.addUserHasOperation(uho);
            userHasOperationRepository.save(uho);
        }
    }

    public void addOperationToUser(String userID, String operationName, String resourceID,
        boolean isDeny) {
        UserHasOperation uho = userHasOperationRepository
            .findByUserIdAndResourceIdAndOperationName(userID, resourceID, operationName);
        if (uho != null) {
            uho.setDeny(isDeny);
        } else {
            User user = userRepository.fetchById(userID);
            Operation operation = operationRepository.findByName(operationName);
            Resource resource = resourceRepository.fetchById(resourceID);
            uho = new UserHasOperation();
            uho.setDeny(isDeny);
            user.addUserHasOperation(uho);
            operation.addUserHasOperation(uho);
            resource.addUserHasOperation(uho);
            userHasOperationRepository.save(uho);

        }
    }

    public void addOperationsToUserFromTemplateID(String userID, String templateID) {
        OpTemplate template = opTemplateRepository.fetchById(templateID);
        addOperationsToUserFromTemplate(userID, template);
    }

    public void addOperationsToUserFromTemplateName(String userID, String templateName) {
        OpTemplate template = opTemplateRepository.findByName(templateName);
        addOperationsToUserFromTemplate(userID, template);
    }

    private void addOperationsToUserFromTemplate(String userID, OpTemplate template) {
        for (OpTemplateHasOperation tho : template.getOpTemplateHasOperations()) {
            if (tho.getResource() == null) {
                addOperationToUser(userID, tho.getOperation().getName(), tho.isDeny());
            } else {
                addOperationToUser(userID, tho.getOperation().getName(), tho.getResource().getId(),
                    tho.isDeny());
            }
        }
    }

    public void addOperationToGroup(String groupID, String operationName, boolean isDeny) {
        GroupHasOperation gho = groupHasOperationRepository.findByGroupIdAndOperationName(groupID, operationName);

        if (gho != null) {
            gho.setDeny(isDeny);
        } else {
            Group group = groupRepository.fetchById(groupID);
            Operation operation = operationRepository.findByName(operationName);
            gho = new GroupHasOperation();
            gho.setDeny(isDeny);
            group.addGroupHasOperation(gho);
            operation.addGroupHasOperation(gho);
            groupHasOperationRepository.save(gho);

        }
    }

    public void addOperationToGroup(String groupID, String operationName, String resourceID,
        boolean isDeny) {
        GroupHasOperation gho = groupHasOperationRepository.findByGroupIdAndOperationName(groupID, operationName);
        if (gho != null) {
            gho.setDeny(isDeny);
        } else {
            Group group = groupRepository.fetchById(groupID);
            Operation operation = operationRepository.findByName(operationName);
            Resource resource = resourceRepository.fetchById(resourceID);
            gho = new GroupHasOperation();
            gho.setDeny(isDeny);
            group.addGroupHasOperation(gho);
            operation.addGroupHasOperation(gho);
            resource.addGroupHasOperation(gho);
            groupHasOperationRepository.save(gho);
        }
    }

    public void addOperationsToGroupFromTemplateID(String groupID, String templateID) {
        OpTemplate template = opTemplateRepository.fetchById(templateID);
        addOperationsToGroupFromTemplate(groupID, template);
    }

    public void addOperationsToGroupFromTemplateName(String groupID, String templateName) {
        OpTemplate template = opTemplateRepository.findByName(templateName);
        addOperationsToGroupFromTemplate(groupID, template);
    }

    private void addOperationsToGroupFromTemplate(String groupID, OpTemplate template) {
        for (OpTemplateHasOperation tho : template.getOpTemplateHasOperations()) {
            if (tho.getResource() == null) {
                addOperationToGroup(groupID, tho.getOperation().getName(), tho.isDeny());
            } else {
                addOperationToGroup(groupID, tho.getOperation().getName(), tho.getResource().getId(),
                    tho.isDeny());
            }
        }
    }

    public void removeOperationFromUser(String userID, String operationName) {
        UserHasOperation uho = userHasOperationRepository.findByUserIdAndOperationName(userID, operationName);
        if (uho != null) {
            userHasOperationRepository.delete(uho);
        }
    }

    public void removeOperationFromUser(String userID, String operationName, String resourceID) {
        UserHasOperation uho = userHasOperationRepository
            .findByUserIdAndResourceIdAndOperationName(userID, resourceID, operationName);
        if (uho != null) {
            userHasOperationRepository.delete(uho);
        }
    }

    public void removeOperationFromGroup(String groupID, String operationName) {
        GroupHasOperation gho = groupHasOperationRepository.findByGroupIdAndOperationName(groupID, operationName);
        if (gho != null) {
            groupHasOperationRepository.delete(gho);
        }
    }

    public void removeOperationFromGroup(String groupID, String operationName, String resourceID) {
        GroupHasOperation gho = groupHasOperationRepository
            .findByGroupIdAndResourceIdAndOperationName(groupID, resourceID, operationName);
        if (gho != null) {
            groupHasOperationRepository.delete(gho);
        }
    }

    public Boolean isPermitted(String userId, String operationName) {
        return isPermitted(userId, operationName, null);
    }

    public Boolean isPermitted(String userId, String operationName, String resourceObjectID) {
        LOGGER.log(
            Level.FINEST,
            "Checking permissions for user ''{0}'', operation ''{1}'' and resource object ID ''{2}''.",
            new String[]{userId, operationName, resourceObjectID});
        User user = userRepository.fetchById(userId);
        if (user.isSuperadmin()) {
            return true;
        }

        Operation operation = operationRepository.findByName(operationName);
        String resourceId =
            (resourceObjectID == null) ? null
                : resourceRepository.findByObjectId(resourceObjectID).getId();

        Boolean retVal = null;
        UserHasOperation uho = (resourceId == null)
            ? userHasOperationRepository.findByUserIdAndOperationName(userId, operationName)
            : userHasOperationRepository.findByUserIdAndResourceIdAndOperationName(userId, resourceId, operationName);
        // Check the user's permission on the operation
        if (uho != null) {
            // First check whether this is a dynamic operation.
            if (operation.isDynamic()) {
                retVal = evaluateDynamicOperation(operation, userId, null,
                    resourceObjectID);
            } else {
                retVal = !uho.isDeny();
            }
        }
        // If no user permission on the operation exists check the permissions for the user groups.
        else {
            List<Group> userGroups = user.getGroups();
            for (Group group : userGroups) {
                Boolean groupPermission;
                groupPermission = isPermittedForGroup(group.getId(), operationName, resourceObjectID);
                if (groupPermission != null) {
                    // Assign the permission we got for the group to the return value only if
                    // a. We haven't found another permission for this user so far
                    // b. The groupPermission is true and we are prioritising positive permissions or
                    // the groupPermission is false and we are prioritising negative permissions.
                    if ((retVal == null) || (groupPermission == prioritisePositive)) {
                        retVal = groupPermission;
                    }

                }
            }
        }

        return retVal;
    }

    public Boolean isPermittedForGroup(String groupID, String operationName) {
        return isPermittedForGroup(groupID, operationName, null);
    }

    public Boolean isPermittedForGroupByResource(String groupID, String operationName,
        String resourceName) {
        LOGGER.log(Level.FINEST,
            "Checking permissions for group {0}, operation {1} and resource with object ID {2}.",
            new String[]{groupID, operationName, resourceName});

        Group group = groupRepository.fetchById(groupID);
        Operation operation = operationRepository.findByName(operationName);
        Boolean retVal = null;
        GroupHasOperation gho = groupHasOperationRepository.findByGroupIdAndOperationName(groupID, operationName);
        if (gho != null) {
            retVal = !gho.isDeny();
        } else if (group.getParent() != null) {
            // If this group is not assigned the operation check the group's
            // parents until a result is found or until no other parent exists.
            retVal = isPermittedForGroup(group.getParent().getId(), operationName, resourceName);
        }

        return retVal;
    }

    public Boolean isPermittedForGroup(String groupID, String operationName,
        String resourceObjectID) {
        LOGGER.log(Level.FINEST,
            "Checking permissions for group {0}, operation {1} and resource with object ID {2}.",
            new String[]{groupID, operationName, resourceObjectID});

        Group group = groupRepository.fetchById(groupID);
        Operation operation = operationRepository.findByName(operationName);
        String resourceId =
            (resourceObjectID == null) ? null
                : resourceRepository.findByObjectId(resourceObjectID).getId();
        Boolean retVal = null;
        GroupHasOperation gho = (resourceId == null)
            ? groupHasOperationRepository.findByGroupIdAndOperationName(groupID, operationName)
            : groupHasOperationRepository
                .findByGroupIdAndResourceIdAndOperationName(groupID, resourceId, operationName);
        if (gho != null) {
            // First check whether this is a dynamic operation.
            if (operation.isDynamic()) {
                retVal = evaluateDynamicOperation(operation, null, groupID,
                    resourceObjectID);
            } else {
                retVal = !gho.isDeny();
            }
        } else if (group.getParent() != null) {
            // If this group is not assigned the operation check the group's
            // parents until a result is found or until no other parent exists.
            retVal = isPermittedForGroup(group.getParent().getId(), operationName, resourceObjectID);
        }

        return retVal;
    }

    private Set<String> getUsersForOperation(String operationName,
        String resourceObjectID, boolean checkUserGroups, boolean getAllowed) {
        Set<String> allUsers = userRepository.getUserIds(false);
        // Superadmin users are allowed the operation by default
        Set<String> returnedUsers = new HashSet<>();
        if (getAllowed) {
            returnedUsers = userRepository.getUserIds(true);
        } else {
            for (String superadminId : userRepository.getUserIds(true)) {
                allUsers.remove(superadminId);
            }
        }

        String resourceId = null;
        if (resourceObjectID != null) {
            resourceId = resourceRepository.findByObjectId(resourceObjectID).getId();
        }

        // First check the permissions of users themselves
        List<UserHasOperation> uhoList;
        if (resourceId == null) {
            uhoList = userHasOperationRepository.findByOperationName(operationName);
        } else {
            uhoList = userHasOperationRepository.findByResourceIdAndOperationName(resourceId, operationName);
        }
        for (UserHasOperation uho : uhoList) {
            allUsers.remove(uho.getUser().getId());

            // Check if operation is dynamic and if yes evaluate the operation
            if (uho.getOperation().isDynamic()) {
                Boolean dynamicResult = evaluateDynamicOperation(uho.getOperation(),
                    uho.getUser().getId(), null, resourceObjectID);
                if ((dynamicResult != null) && (dynamicResult.booleanValue() == getAllowed)) {
                    returnedUsers.add(uho.getUser().getId());
                }
            } else if (!uho.isDeny() == getAllowed) {
                returnedUsers.add(uho.getUser().getId());
            }
        }

        // Then iterate over the remaining users to check group permissions
        if (checkUserGroups) {
            // Using Iterator to iterate over allUsers in order to avoid
            // ConcurrentModificationException caused by user removal in the for loop
            Iterator<String> userIt = allUsers.iterator();
            while (userIt.hasNext()) {
                String userId = userIt.next();
                List<Group> userGroups = userRepository.fetchById(userId).getGroups();
                Boolean userPermission = null;
                for (Group group : userGroups) {
                    Boolean groupPermission;
                    if (resourceObjectID == null) {
                        groupPermission = isPermittedForGroup(group.getId(), operationName);
                    } else {
                        groupPermission = isPermittedForGroup(group.getId(), operationName, resourceObjectID);
                    }
                    // We have the following cases depending on the group permission:
                    // a. If it was positive and we are prioritising positive permissions the user
                    // is allowed and we end the check for this user. The user will be added to
                    // the returned users if getAllowed == true.
                    // b. If it was negative and we are prioritising negative permissions the user
                    // is not allowed and we end the check for this user. The user will be added to
                    // the returned users if getAllowed == false.
                    // c. In all other cases we wait until the rest of the user groups are checked
                    // before we make a final decision. For this reason we assign the groupPermission
                    // to the userPermission variable to be checked after group check is finished.
                    if (groupPermission != null) {
                        userIt.remove();
                        if (groupPermission.booleanValue() == prioritisePositive) {
                            if (groupPermission.booleanValue() == getAllowed) {
                                returnedUsers.add(userId);
                            }
                            userPermission = null;
                            break;
                        } else {
                            userPermission = groupPermission;
                        }
                    }
                }
                if ((userPermission != null) && (userPermission.booleanValue() == getAllowed)) {
                    returnedUsers.add(userId);
                }
            }
        }

        return returnedUsers;
    }

    public Set<String> getAllowedUsersForOperation(String operationName,
        boolean checkUserGroups) {
        return getUsersForOperation(operationName, null, checkUserGroups, true);
    }

    public Set<String> getAllowedUsersForOperation(String operationName, String resourceObjectID,
        boolean checkUserGroups) {
        return getUsersForOperation(operationName, resourceObjectID, checkUserGroups, true);
    }

    public Set<String> getBlockedUsersForOperation(String operationName,
        boolean checkUserGroups) {
        return getUsersForOperation(operationName, null, checkUserGroups, false);
    }

    public Set<String> getBlockedUsersForOperation(String operationName, String resourceObjectID,
        boolean checkUserGroups) {
        return getUsersForOperation(operationName, resourceObjectID, checkUserGroups, false);
    }

    private Set<String> getGroupsForOperation(String operationName, String resourceObjectID,
        boolean checkAncestors, boolean getAllowed) {
        Set<String> allGroups = groupRepository.getAllIds();
        Set<String> returnedGroups = new HashSet<>();

        String resourceId = null;
        if (resourceObjectID != null) {
            resourceId = resourceRepository.findByObjectId(resourceObjectID).getId();
        }
        List<GroupHasOperation> ghoList;
        if (resourceId == null) {
            ghoList = groupHasOperationRepository.findByOperationName(operationName);
        } else {
            ghoList = groupHasOperationRepository.
                findByResourceIdAndOperationName(resourceId, operationName);
        }
        for (GroupHasOperation gho : ghoList) {
            allGroups.remove(gho.getGroup().getId());

            // Check if operation is dynamic and if yes evaluate the operation
            if (gho.getOperation().isDynamic()) {
                Boolean dynamicResult = evaluateDynamicOperation(gho.getOperation(),
                    null, gho.getGroup().getId(), null);
                if ((dynamicResult != null) && (dynamicResult.booleanValue() == getAllowed)) {
                    returnedGroups.add(gho.getGroup().getId());
                }
            } else if (!gho.isDeny() == getAllowed) {
                returnedGroups.add(gho.getGroup().getId());
            }
        }

        // Check the ancestors of the remaining groups if so requested
        if (checkAncestors) {
            for (String groupId : allGroups) {
                Boolean groupPermission;
                if (resourceObjectID == null) {
                    groupPermission = isPermittedForGroup(groupId, operationName);
                } else {
                    groupPermission = isPermittedForGroup(groupId, operationName, resourceObjectID);
                }
                if ((groupPermission != null) && (groupPermission.booleanValue() == getAllowed)) {
                    returnedGroups.add(groupId);
                }
            }
        }

        return returnedGroups;
    }

    public Set<String> getAllowedGroupsForOperation(String operationName, boolean checkAncestors) {
        return getGroupsForOperation(operationName, null, checkAncestors, true);
    }

    public Set<String> getAllowedGroupsForOperation(String operationName,
        String resourceObjectID, boolean checkAncestors) {
        return getGroupsForOperation(operationName, resourceObjectID, checkAncestors, true);
    }

    public Set<String> getBlockedGroupsForOperation(String operationName, boolean checkAncestors) {
        return getGroupsForOperation(operationName, null, checkAncestors, false);
    }

    public Set<String> getBlockedGroupsForOperation(String operationName,
        String resourceObjectID, boolean checkAncestors) {
        return getGroupsForOperation(operationName, resourceObjectID, checkAncestors, false);
    }

    private Boolean evaluateDynamicOperation(Operation operation,
        String userID, String groupID, String resourceObjectID) {
        LOGGER.log(Level.FINEST, "Evaluating dynamic operation ''{0}''.",
            operation.getName());

        Boolean retVal;
        String algorithm = operation.getDynamicCode();
        // Create a BeanShell interpreter for this operation.
        Interpreter i = new Interpreter();
        // Pass parameters to the algorithm.
        try {
            i.set("userID", userID);
            i.set("groupID", groupID);
            i.set("resourceObjectID", resourceObjectID);
            i.eval(algorithm);
            retVal = ((Boolean) i.get("retVal")).booleanValue();
        } catch (EvalError ex) {
            // Catching the EvalError in order to convert it to
            // a RuntimeException which will also rollback the transaction.
            throw new DynamicOperationException(
                "Error evaluating dynamic operation '"
                    + operation.getName() + "'.");
        }

        return retVal;
    }

    public Set<String> getPermittedOperationsForUser(String userID, boolean checkUserGroups) {
        User user = userRepository.fetchById(userID);
        return getOperationsForUser(user, null, checkUserGroups);
    }

    public Set<String> getPermittedOperationsForUser(String userID, String resourceObjectID,
        boolean checkUserGroups) {
        User user = userRepository.fetchById(userID);
        Resource resource = resourceRepository.findByObjectId(resourceObjectID);
        return getOperationsForUser(user, resource, checkUserGroups);
    }

    private Set<String> getOperationsForUser(User user, Resource resource,
        boolean checkUserGroups) {
        Set<String> allowedOperations = new HashSet<>();
        Set<String> deniedOperations = new HashSet<>();

        // If the user is a superadmin then they are allowed all operations
        if (user.isSuperadmin()) {
            for (Operation operation : operationRepository.findAll()) {
                allowedOperations.add(operation.getName());
            }
        } else {
            // Check operations attributed to the user
            for (UserHasOperation uho : user.getUserHasOperations()) {
                if (uho.getResource() == resource) {
                    if ((uho.getOperation().isDynamic() && evaluateDynamicOperation(
                        uho.getOperation(), user.getId(), null, resource.getObjectId()))
                        || (!uho.getOperation().isDynamic() && !uho.isDeny())) {
                        allowedOperations.add(uho.getOperation().getName());
                    } else if ((uho.getOperation().isDynamic() && !evaluateDynamicOperation(
                        uho.getOperation(), user.getId(), null, resource.getObjectId()))
                        || (!uho.getOperation().isDynamic() && uho.isDeny())) {
                        deniedOperations.add(uho.getOperation().getId());
                    }
                }
            }
            if (checkUserGroups) {
                // Then check operations the user may have via their groups
                Set<String> allowedGroupOperations = new HashSet<>();
                Set<String> deniedGroupOperations = new HashSet<>();
                // First get all the operations allowed or denied through the user groups
                for (Group group : user.getGroups()) {
                    while (group != null) {
                        allowedGroupOperations.addAll(getOperationsForGroup(group, resource, true));
                        deniedGroupOperations.addAll(getOperationsForGroup(group, resource, false));
                        group = group.getParent();
                    }
                }
                // And then check for each allowed operation if it is explicitly denied
                // to the user or if it is denied through another group (only if prioritisePositive == false)
                for (String groupOperation : allowedGroupOperations) {
                    if (!deniedOperations.contains(groupOperation)
                        && (prioritisePositive || (!deniedGroupOperations.contains(groupOperation)))) {
                        allowedOperations.add(groupOperation);
                    }
                }
            }
        }

        return allowedOperations;
    }

    private Set<String> getOperationsForGroup(Group group, Resource resource, boolean allowed) {
        Set<String> retVal = new HashSet<>();
        for (GroupHasOperation gho : group.getGroupHasOperations()) {
            if (gho.getResource() == resource) {
                String resourceObjectID = (resource != null) ? resource.getObjectId() : null;
                if ((gho.getOperation().isDynamic() &&
                    (evaluateDynamicOperation(gho.getOperation(), null, group.getId(), resourceObjectID)
                        == allowed))
                    || (!gho.getOperation().isDynamic() && (!gho.isDeny() == allowed))) {
                    retVal.add(gho.getOperation().getName());
                }
            }
        }
        return retVal;
    }

    public Set<ResourceDTO> getResourceForOperation(String userID, String... operations) {
        return getResourceForOperation(userID, true, false, operations);
    }

    public Set<ResourceDTO> getResourceForOperation(String userID,
        boolean getAllowed, String... operations) {
        return getResourceForOperation(userID, getAllowed, false, operations);
    }

    public Set<ResourceDTO> getResourceForOperation(String userID,
        boolean getAllowed, boolean checkUserGroups, String... operations) {
        Set<ResourceDTO> resourceDTOList = new HashSet<>();
        User user = userRepository.fetchById(userID);
        for (UserHasOperation uho : user.getUserHasOperations()) {
            if (uho.isDeny() != getAllowed && Stream.of(operations).anyMatch(o -> o.equals(uho.getOperation().getName()))) {
                resourceDTOList
                    .add(resourceMapper.mapToDTO(
                        resourceRepository.fetchById(uho.getResource().getId())));
            }
        }
        /* also the resources of the groups the user belongs to should be retrieved */
        if (checkUserGroups) {
            for (Group group : user.getGroups()) {
                for (GroupHasOperation gho : group.getGroupHasOperations()) {
                    if (gho.isDeny() != getAllowed && Stream.of(operations).anyMatch(o -> o.equals(gho.getOperation().getName()))) {
                        resourceDTOList.add(
                            resourceMapper.mapToDTO(resourceRepository.fetchById((gho.getResource().getId()))));
                    }
                }
            }
        }
        return resourceDTOList;
    }

    public OperationDTO getOperationByID(String operationID) {
        Operation o = operationRepository.fetchById(operationID);
        if (o != null) {
            return operationMapper.mapToDTO(o);
        } else {
            return null;
        }
    }

    public List<String> getGroupIDsByOperationAndUser(String operationName, String userId) {
        return null;
    }

    public List<GroupHasOperationDTO> getGroupOperations(String groupName) {

        return groupHasOperationMapper.mapToDTO(
            groupHasOperationRepository.findByGroupName(groupName));
    }

    public List<GroupHasOperationDTO> getGroupOperations(List<String> groupNames) {
        List<GroupHasOperation> entities = new ArrayList<>();
        groupNames
            .forEach(groupName ->
                entities.addAll(groupHasOperationRepository.findByGroupName(groupName))
            );

        return groupHasOperationMapper.mapToDTO(entities);
    }
}