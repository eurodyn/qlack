package com.eurodyn.qlack.fuse.cm.service;

import com.eurodyn.qlack.fuse.cm.dto.FolderDTO;
import com.eurodyn.qlack.fuse.cm.dto.NodeDTO;
import com.eurodyn.qlack.fuse.cm.enums.NodeType;
import com.eurodyn.qlack.fuse.cm.enums.RelativesType;
import com.eurodyn.qlack.fuse.cm.exception.QAncestorFolderLockException;
import com.eurodyn.qlack.fuse.cm.exception.QDescendantNodeLockException;
import com.eurodyn.qlack.fuse.cm.exception.QFileNotFoundException;
import com.eurodyn.qlack.fuse.cm.exception.QNodeLockException;
import com.eurodyn.qlack.fuse.cm.exception.QSelectedNodeLockException;
import com.eurodyn.qlack.fuse.cm.mappers.NodeMapper;
import com.eurodyn.qlack.fuse.cm.model.Node;
import com.eurodyn.qlack.fuse.cm.model.NodeAttribute;
import com.eurodyn.qlack.fuse.cm.repository.NodeRepository;
import com.eurodyn.qlack.fuse.cm.util.CMConstants;
import java.util.Calendar;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @author European Dynamics
 */
@Service
@Transactional
public class ConcurrencyControlService {

  private final NodeMapper nodeMapper;
  private final NodeRepository nodeRepository;
  private final String commonInvalidLockTokenMsg = "is locked and an invalid lock token was passed; the folder cannot be locked.";

  @Autowired
  public ConcurrencyControlService(NodeMapper nodeMapper, NodeRepository nodeRepository) {
    this.nodeMapper = nodeMapper;
    this.nodeRepository = nodeRepository;
  }

  /**
   * Locks a node, so that it cannot be modified. If the node is already locked then an exception is thrown.
   *
   * @param nodeID The id of the node to be locked.
   * @param lockToken A token used to lock the node. All future operations altering this node will be blocked while the node is locked
   * unless the token used to lock the node is passed to the operation.
   * @param lockChildren Whether child nodes of this node should also be locked
   * @param userID The ID of the user who locked this node. This will be used simply as metadata for the lock and will not be taken into
   * account when checking if a locked node can be edited.
   * @throws QNodeLockException If the specified node is already locked
   */
  public void lock(String nodeID, String lockToken, boolean lockChildren, String userID) throws QNodeLockException {
    Node node = fetchNode(nodeID, "The folder/file you want to lock does not exist");

    checkIfLockable(node);

    NodeDTO selConflict = getSelectedNodeWithLockConflict(nodeID, lockToken);
    if (selConflict != null && selConflict.getName() != null) {
      throw new QSelectedNodeLockException("The selected folder " + commonInvalidLockTokenMsg, selConflict.getId(), selConflict.getName());
    }

    if (node.getParent() != null) {
      NodeDTO ancConflict = getAncestorFolderWithLockConflict(nodeID, lockToken);
      if (ancConflict != null && ancConflict.getId() != null) {
        throw new QAncestorFolderLockException("An ancestor folder " + commonInvalidLockTokenMsg, ancConflict.getId(),
          ancConflict.getName());
      }
    }

    if (!CollectionUtils.isEmpty(node.getChildren())) {
      NodeDTO desConflict = getDescendantNodeWithLockConflict(nodeID, lockToken);
      if (desConflict != null && desConflict.getId() != null) {
        throw new QDescendantNodeLockException("An descendant node " + commonInvalidLockTokenMsg, desConflict.getId(),
          desConflict.getName());
      }
    }

    node.setLockToken(lockToken);
    node.setAttribute(CMConstants.ATTR_LOCKED_ON, String.valueOf(Calendar.getInstance().getTimeInMillis()));
    node.setAttribute(CMConstants.ATTR_LOCKED_BY, userID);
    nodeRepository.save(node);

    // In case the lockChildren variable is true all the children of the folder will recursively be locked
    if (lockChildren) {
      for (Node child : node.getChildren()) {
        lock(child.getId(), lockToken, true, userID);
      }
    }
  }

  /**
   * Unlocks a node. Before this method attempts to unlock a node it checks to see whether it has been previously locked or not therefore
   * you can safely use it even if you do not know the current lock state of a node.
   *
   * @param nodeID The id of the node to be unlocked.
   * @param lockToken The token with which the node was locked which will be used to unlock the node
   * @param overrideLock If false then this method will not attempt to use the passed lock token but will just unlock the node by setting
   * null as the current token.
   * @param userID The ID of the user who locked this node. This will be used simply as metadata for the lock and will not be taken into
   * account when checking if a locked node can be edited.
   * @throws QNodeLockException If the node cannot be unlocked with the passed token
   */
  public void unlock(String nodeID, String lockToken, boolean overrideLock, String userID) throws QNodeLockException {
    Node node = fetchNode(nodeID, "The folder/file you want to unlock does not exist");

    if (!overrideLock) {
      // Check whether there is a lock conflict with the current node.
      NodeDTO selConflict = getSelectedNodeWithLockConflict(nodeID, lockToken);
      if (selConflict != null && selConflict.getName() != null) {
        throw new QSelectedNodeLockException(
          "The selected folder is locked and an invalid lock token was passed; the folder cannot be unlocked.",
          selConflict.getId(), selConflict.getName());
      }
    }

    node.setLockToken(null);
    // remove the attribute itself
    node.removeAttribute(CMConstants.ATTR_LOCKED_ON);
    node.removeAttribute(CMConstants.ATTR_LOCKED_BY);
    nodeRepository.save(node);

    // In case an override lock is requested the node will be locked again using the provided lock token
    if (overrideLock) {
      lock(nodeID, lockToken, false, userID);
    }
  }

  /**
   * Checks whether a specific node has lock conflict, by examining if the provided lock token, is different from the lockTocken of the
   * node.
   *
   * @param nodeID The UUID of the node to check.
   * @return The NodeDTO with the conflict or null.
   */
  public NodeDTO getSelectedNodeWithLockConflict(String nodeID, String lockToken) {
    Node node = fetchNode(nodeID, "The folder/file you want to unlock does not exist");
    return commonLockConflictCheck(node, lockToken);
  }

  /**
   * Checks whether an ancestor folder has lock conflict and returns the first one it will find. A lock conflict in this case happens when
   * the provided lock token, is different from the lockTocken of a specific ascendant folder.
   *
   * @param nodeID The UUID of the node to check.
   * @return The FolderDTO of the first locked ancestor folders with which there is a conflict
   */
  public FolderDTO getAncestorFolderWithLockConflict(String nodeID, String lockToken) {
    // Find the current node.
    Node node = nodeRepository.fetchById(nodeID);

    // Check whether the current node has lock and is conflicting.
    // Returns the first folder it will find with conflict.

    commonLockConflictCheck(node, lockToken);
    if (node.getLockToken() != null && !node.getLockToken().equals(lockToken)) {
      return nodeMapper.mapToFolderDTO(node, RelativesType.LAZY, false);
    }

    // In case the current node does not have conflict but it does have
    // parents call the recursive function again.
    return node.getParent() != null ? getAncestorFolderWithLockConflict(node.getParent().getId(), lockToken) : null;
  }

  /**
   * Checks whether a descendant node has lock conflict  and returns the first one it will find.  A lock conflict in this case happens
   * when the provided lock token, is different from the lockTocken of a descendant node.
   *
   * @param nodeID The UUID of the node to check.
   * @return The first of the locked descendant NodeDTO with which there is a conflict
   */
  public NodeDTO getDescendantNodeWithLockConflict(String nodeID, String lockToken) {
    // Find the current node.
    Node node = nodeRepository.fetchById(nodeID);
    for (Node child : node.getChildren()) {
      /*
       Check whether the current node has a conflicting lock.
       Returns the first node it will find with conflict, otherwise recursively look for conflicts with descendant nodes.
      */
      NodeDTO nodeDTO = commonLockConflictCheck(child, lockToken);
      return nodeDTO != null ? nodeDTO : getDescendantNodeWithLockConflict(child.getId(), lockToken);
    }
    return null;
  }

  private Node fetchNode(String nodeId, String errorMsg) throws QNodeLockException {
    Node node = nodeRepository.fetchById(nodeId);
    if (node == null) {
      throw new QFileNotFoundException(errorMsg);
    }
    return node;
  }

  private NodeDTO commonLockConflictCheck(Node node, String lockToken) {
    if (node.getLockToken() != null && !node.getLockToken().equals(lockToken)) {
      if (node.getType().equals(NodeType.FOLDER)) {
        return nodeMapper.mapToFolderDTO(node, RelativesType.LAZY, false);
      } else if (node.getType().equals(NodeType.FILE)) {
        return nodeMapper.mapToFileDTO(node, false);
      }
    }
    return null;
  }

  private void checkIfLockable(Node node) throws QNodeLockException {
    NodeAttribute lockable = node.getAttribute(CMConstants.LOCKABLE);

    if (!Boolean.valueOf(lockable.getValue())) {
      throw new QNodeLockException("Folder is not lockable");
    }
  }
}
