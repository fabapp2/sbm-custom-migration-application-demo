#!/bin/sh

scriptDir=$(dirname "${BASH_SOURCE[0]}")
seclibDir="${scriptDir}/seclib-core"
seclibUpgradeDir="${scriptDir}/seclib-upgrade-v5-v6"
businessServiceDir="${scriptDir}/business-service"
sbmGitHubUrl=https://github.com/spring-projects-experimental/spring-boot-migrator.git
sbmDir="${scriptDir}/spring-boot-migrator"

rm -rf $sbmDir

git clone $sbmGitHubUrl

pushd $sbmDir
  pushd components/sbm-core
    echo "build sbm-core"
    mvn clean install
  popd
popd

pushd $seclibDir
  echo "build seclib-core"
  mvn clean install
popd

pushd $seclibUpgradeDir
  echo "build seclip-upgrade-tool"
  mvn clean install
popd

#pushd $businessServiceDir
#  echo ""
#  mvn clean install
#popd