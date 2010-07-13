----------------
Release process:
----------------
This section explains the steps involved in releasing the singleton component. The release process is similar to what's been explained here http://community.jboss.org/wiki/DavidsMavenReleaseProcess

1) First step during the release is to look for thirdparty (anything outside of the singleton component) libraries that need to be released. For this, let's "cd" to the root of the singleton component and then run:

jpai@jpai-laptop:/NotBackedUp/jpai/business/jboss/wc/jbossas/projects/ejb3/components/singleton/jboss-ejb3-singleton$ grep -R -B3 SNAPSHOT --include=pom.xml ./
./spi/pom.xml-    <parent>
./spi/pom.xml-        <groupId>org.jboss.ejb3.singleton</groupId>
./spi/pom.xml-        <artifactId>jboss-ejb3-singleton-parent</artifactId>
./spi/pom.xml:        <version>1.0.0-SNAPSHOT</version>
--
./docs/reference/pom.xml-    
./docs/reference/pom.xml-    <groupId>org.jboss.ejb3.singleton</groupId>
./docs/reference/pom.xml-    <artifactId>jboss-ejb3-singleton-reference-doc-${translation}</artifactId>
./docs/reference/pom.xml:    <version>1.0.0-SNAPSHOT</version>
--
./proxy/pom.xml-    <parent>
./proxy/pom.xml-        <groupId>org.jboss.ejb3.singleton</groupId>
./proxy/pom.xml-        <artifactId>jboss-ejb3-singleton-parent</artifactId>
./proxy/pom.xml:        <version>1.0.0-SNAPSHOT</version>
--
./aop-impl/pom.xml-    <parent>
./aop-impl/pom.xml-        <groupId>org.jboss.ejb3.singleton</groupId>
./aop-impl/pom.xml-        <artifactId>jboss-ejb3-singleton-parent</artifactId>
./aop-impl/pom.xml:        <version>1.0.0-SNAPSHOT</version>
--
./aop-impl/pom.xml-    
./aop-impl/pom.xml-    <properties>
./aop-impl/pom.xml-        <version.ejb3.singleton.impl>${project.version}</version.ejb3.singleton.impl>
./aop-impl/pom.xml:	    <version.ejb3.core>1.5.0-SNAPSHOT</version.ejb3.core>
--
./impl/pom.xml-    <parent>
./impl/pom.xml-        <groupId>org.jboss.ejb3.singleton</groupId>
./impl/pom.xml-        <artifactId>jboss-ejb3-singleton-parent</artifactId>
./impl/pom.xml:        <version>1.0.0-SNAPSHOT</version>
--
./deployer/pom.xml-    <parent>
./deployer/pom.xml-        <groupId>org.jboss.ejb3.singleton</groupId>
./deployer/pom.xml-        <artifactId>jboss-ejb3-singleton-parent</artifactId>
./deployer/pom.xml:        <version>1.0.0-SNAPSHOT</version>
--
./testsuite/pom.xml-    <parent>
./testsuite/pom.xml-        <groupId>org.jboss.ejb3.singleton</groupId>
./testsuite/pom.xml-        <artifactId>jboss-ejb3-singleton-parent</artifactId>
./testsuite/pom.xml:        <version>1.0.0-SNAPSHOT</version>
--
./pom.xml-    <!-- Artifact Information  -->
./pom.xml-    <groupId>org.jboss.ejb3.singleton</groupId>
./pom.xml-    <artifactId>jboss-ejb3-singleton-parent</artifactId>
./pom.xml:    <version>1.0.0-SNAPSHOT</version>

As can be seen in the above (sample) output, the only thirdparty SNAPSHOT dependency is on jboss-ejb3-core:

./aop-impl/pom.xml:	    <version.ejb3.core>1.5.0-SNAPSHOT</version.ejb3.core>

2) The next step is to release all the thirdparty SNAPSHOT dependencies separately. The process of releasing such dependencies is out of scope of this document.

3) After releasing such thirdparty dependencies, update (and commit) the appropriate pom.xml files in the singleton component to use the latest released versions.

4) Repeat step#1 once more (for a sanity check).

Once we have update all the necessary singleton pom.xml files to use non-SNAPSHOT dependencies of thirdparty libraries, the next step is to actually start the release process.

5) Identify the version to which you want to release singleton component. Let's assume we want to release singleton from 1.0.0-SNAPSHOT to 1.0.0-alpha-X.

6) Replace all singleton component pom versions from 1.0.0-SNAPSHOT to 1.0.0-alpha-X. I usually use:

jpai@jpai-laptop:/NotBackedUp/jpai/business/jboss/wc/jbossas/projects/ejb3/components/singleton/jboss-ejb3-singleton$ perl -pi -e 's/1.0.0-SNAPSHOT/1.0.0-alpha-X/g' `find . -name pom.xml`

7) Do a git diff from the singleton component root and make sure only the appropriate singleton pom versions have been updated.

8) Do a build of the current state:

mvn clean install

9) Upon a successful build, the next step is to commit the changes made in step#6:

git commit --no-verify -m "Prepare for release of 1.0.0-alpha-X"

10) Next, let's tag the current state of singleton:

git tag -a "1.0.0-alpha-X" -m "Tag 1.0.0-alpha-X"

	-a : This specifies the tag name
	-m : This is the message (similar to commit messages) which will be applied for the change

11) Now that we have tagged it, let's release the artifacts to the maven repo. We *don't* use the Maven release plugin. Instead we use the following command for the release:

jpai@jpai-laptop:/NotBackedUp/jpai/business/jboss/wc/jbossas/projects/ejb3/components/singleton/jboss-ejb3-singleton$ mvn clean deploy -DaltDeploymentRepository=jboss-releases-repository::default::https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/

The -DaltDeploymentRepository has the following parts:
	jboss-releases-repository : This is the name of the repo to which we are releasing (this value won't change, atleast not in the near future)
	default : The "layout" of the maven repo (this value won't change)
	https://repository.jboss.org/nexus/service/local/staging/deploy/maven2/ : The URL of the release repo (this value won't change, atleast not in the near future)

13) After step#12 completes successfully, the next step involves "Promoting" the release through the nexus repo. Please follow the JBoss Wiki doc for the details <TODO: Link to the doc>

14) The next step is to change the version numbers in singleton pom to the next development (SNAPSHOT) version. Let's assume we are going to 1.0.0-SNAPSHOT. So we'll have to replace all 1.0.0-alpha-X with 1.0.0-SNAPSHOT (make sure that you check that only singleton modules' version numbers change and not any other thirdparty module which is also at 1.0.0-alpha-X):

jpai@jpai-laptop:/NotBackedUp/jpai/business/jboss/wc/jbossas/projects/ejb3/components/singleton/jboss-ejb3-singleton$ perl -pi -e 's/1.0.0-alpha-X/1.0.0-SNAPSHOT/g' `find . -name pom.xml`

15) Run a clean build with these changes:

mvn clean install

16) Commit the changes done in step#14

git commit --no-verify -m "Next development version" ./

17) Push everything including the newly created tag, to the remote repo:

git push --tags origin master

That's it, the release is over! Time to get some sleep ;)
  	
