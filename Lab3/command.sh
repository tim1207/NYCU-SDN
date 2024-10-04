# Build ONOS Application Archetypes
# Steps:
# Specify ONOS version:
export ONOS_POM_VERSION=2.7.0
# Build archetypes:
cd $ONOS_ROOT/tools/package/archetypes
mvn clean install -DskipTests
# -DskipTests: Skip running tests of the project.


onos-create-app

# Build ONOS Application Template
groupId: nycu.winlab
artifactId: bridge-app
version: 1.0-SNAPSHOT
package: nycu.winlab.bridge
Y: enter

# ~/onos/tools/package/archetypes/bridge-app
# or in bridge-app

sudo vim pom.xml
# in line 34
```
<properties>
   <onos.app.name>nycu.winlab.bridge</onos.app.name>
   <onos.app.title>Learing Bridge APP</onos.app.title>
   <onos.app.origin>WinLab, nycu</onos.app.origin>
   <onos.app.category>default</onos.app.category>
   <onos.app.url>http://onosproject.org</onos.app.url>
   <onos.app.readme>ONOS OSGi bundle archetype.</onos.app.readme>
</properties>
```
# in bridge-app
mvn clean install -DskipTests
ls target

# another terminal Run onos
cd $ONOS_ROOT
ok clean
# another terminal install onos APP
cd $ONOS_ROOT
onos-app localhost install! target/bridge-app-1.0-SNAPSHOT.oar

onos-app localhost uninstall nycu.winlab.bridge

# Rebuild application of new version:
mvn clean install -DskipTests


# Deactivate application of old version on ONOS:
onos-app localhost deactivate <onos.app.name>
# <onos.app.name> is set in your pom.xml. e.g. nycu.winlab.bridge
# Uninstall application of old version:
onos-app localhost uninstall <onos.app.name>
# Install and activate application of new version:
# Hint install! => install and active
onos-app localhost install! target/<artifactId>-<version>.oar

sudo mn --controller=remote,127.0.0.1:6653 \
      --topo=tree,depth=2 \
      --switch=ovs,protocols=OpenFlow14












