/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs.server.namenode.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DFSTestUtil;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.hdfs.protocol.SnapshottableDirectoryStatus;
import org.apache.hadoop.hdfs.server.namenode.FSNamesystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSnapshottableDirListing {

  static final long seed = 0;
  static final short REPLICATION = 3;
  static final long BLOCKSIZE = 1024;

  private final Path root = new Path("/");
  private final Path dir1 = new Path("/TestSnapshot1");
  private final Path dir2 = new Path("/TestSnapshot2");
  
  Configuration conf;
  MiniDFSCluster cluster;
  FSNamesystem fsn;
  DistributedFileSystem hdfs;
  
  @Before
  public void setUp() throws Exception {
    conf = new Configuration();
    cluster = new MiniDFSCluster(conf, REPLICATION, true, null);
    cluster.waitActive();
    fsn = cluster.getNameNode().getNamesystem();
    hdfs = (DistributedFileSystem) cluster.getFileSystem();
    hdfs.mkdirs(dir1);
    hdfs.mkdirs(dir2);
  }

  @After
  public void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
    }
  }
  
  /**
   * Test listing all the snapshottable directories
   */
  @Test
  public void testListSnapshottableDir() throws Exception {
    // Initially there is no snapshottable directories in the system
    SnapshottableDirectoryStatus[] dirs = hdfs.getSnapshottableDirListing();
    assertNull(dirs);
    
    // Make dir1 as snapshottable
    hdfs.allowSnapshot(dir1.toString());
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(1, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    // There is no snapshot for dir1 yet
    assertEquals(0, dirs[0].getSnapshotNumber());
    
    // Make dir2 as snapshottable
    hdfs.allowSnapshot(dir2.toString());
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    // There is no snapshot for dir2 yet
    assertEquals(0, dirs[1].getSnapshotNumber());
    
    // Create dir3
    final Path dir3 = new Path("/TestSnapshot3");
    hdfs.mkdirs(dir3);
    hdfs.allowSnapshot(dir3.toString());
    // Move dir3 under dir2
    hdfs.rename(dir3, dir2);
    final Path dir2dir3 = new Path(dir2, dir3.getName());
    // We have 3 snapshottable dir
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(3, dirs.length);
    assertEquals(dir1.getName(), dirs[0].getDirStatus().getLocalName());
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2.getName(), dirs[1].getDirStatus().getLocalName());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(dir3.getName(), dirs[2].getDirStatus().getLocalName());
    assertEquals(dir2dir3, dirs[2].getFullPath());
    
    // Create sub-dirs under dir1
    Path sub1 = new Path(dir1, "sub1");
    Path file1 =  new Path(sub1, "file1");
    Path sub2 = new Path(dir1, "sub2");
    Path file2 =  new Path(sub2, "file2");
    DFSTestUtil.createFile(hdfs, file1, BLOCKSIZE, REPLICATION, seed);
    DFSTestUtil.createFile(hdfs, file2, BLOCKSIZE, REPLICATION, seed);
    
    // Make sub1 and sub2 snapshottable
    hdfs.allowSnapshot(sub1.toString());
    hdfs.allowSnapshot(sub2.toString());
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(5, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(dir2dir3, dirs[2].getFullPath());
    assertEquals(sub1, dirs[3].getFullPath());
    assertEquals(sub2, dirs[4].getFullPath());
    
    // reset sub1
    hdfs.disallowSnapshot(sub1.toString());
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(4, dirs.length);
    assertEquals(dir1, dirs[0].getFullPath());
    assertEquals(dir2, dirs[1].getFullPath());
    assertEquals(dir2dir3, dirs[2].getFullPath());
    assertEquals(sub2, dirs[3].getFullPath());
    
    // Remove dir1, both dir1 and sub2 will be removed
    hdfs.delete(dir1, true);
    dirs = hdfs.getSnapshottableDirListing();
    assertEquals(2, dirs.length);
    assertEquals(dir2, dirs[0].getFullPath());
    assertEquals(dir2dir3, dirs[1].getFullPath());
  }
  
  /**
   * Test the listing with different user names to make sure only directories
   * that are owned by the user are listed.
   */
  @Test
  public void testListWithDifferentUser() throws Exception {
    // first make dir1 and dir2 snapshottable
    hdfs.allowSnapshot(dir1.toString());
    hdfs.allowSnapshot(dir2.toString());
    hdfs.setPermission(root, FsPermission.valueOf("-rwxrwxrwx"));
    
    // create two dirs and make them snapshottable under the name of user1
    UserGroupInformation ugi1 = UserGroupInformation.createUserForTesting(
        "user1", new String[] { "group1" });
    DistributedFileSystem fs1 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi1, conf);
    Path dir1_user1 = new Path("/dir1_user1");
    Path dir2_user1 = new Path("/dir2_user1");
    fs1.mkdirs(dir1_user1);
    fs1.mkdirs(dir2_user1);
    fs1.allowSnapshot(dir1_user1.toString());
    fs1.allowSnapshot(dir2_user1.toString());
    
    // user2
    UserGroupInformation ugi2 = UserGroupInformation.createUserForTesting(
        "user2", new String[] { "group2" });
    DistributedFileSystem fs2 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(ugi2, conf);
    Path dir_user2 = new Path("/dir_user2");
    Path subdir_user2 = new Path(dir_user2, "subdir");
    fs2.mkdirs(dir_user2);
    fs2.mkdirs(subdir_user2);
    fs2.allowSnapshot(dir_user2.toString());
    fs2.allowSnapshot(subdir_user2.toString());
    
    // super user
    String supergroup = conf.get(
        DFSConfigKeys.DFS_PERMISSIONS_SUPERUSERGROUP_KEY,
        DFSConfigKeys.DFS_PERMISSIONS_SUPERUSERGROUP_DEFAULT);
    UserGroupInformation superUgi = UserGroupInformation.createUserForTesting(
        "superuser", new String[] { supergroup });
    DistributedFileSystem fs3 = (DistributedFileSystem) DFSTestUtil
        .getFileSystemAs(superUgi, conf);
    
    // list the snapshottable dirs for superuser
    SnapshottableDirectoryStatus[] dirs = fs3.getSnapshottableDirListing();
    // 6 snapshottable dirs: dir1, dir2, dir1_user1, dir2_user1, dir_user2, and
    // subdir_user2
    assertEquals(6, dirs.length);
    
    // list the snapshottable dirs for user1
    dirs = fs1.getSnapshottableDirListing();
    // 2 dirs owned by user1: dir1_user1 and dir2_user1
    assertEquals(2, dirs.length);
    assertEquals(dir1_user1, dirs[0].getFullPath());
    assertEquals(dir2_user1, dirs[1].getFullPath());
    
    // list the snapshottable dirs for user2
    dirs = fs2.getSnapshottableDirListing();
    // 2 dirs owned by user2: dir_user2 and subdir_user2
    assertEquals(2, dirs.length);
    assertEquals(dir_user2, dirs[0].getFullPath());
    assertEquals(subdir_user2, dirs[1].getFullPath());
  }
}
