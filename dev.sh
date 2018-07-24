#!/bin/bash

# Exit on failure
set -e

MY_SELF=$0
MY_CMD=$1
MY_ARG=$2

function usage() {
    cat << EOF
usage: $0 [command] [arguments]

  sonarcube        update sonarcube
  debug		   start connecting to remote debuger on port 8000
  test [name]      run a specific test
EOF
}

case "$MY_CMD" in
    "sonarcube")
        # This assumes that the 2 following variables are defined:
        # - SONAR_HOST_URL => should point to the public URL of the SQ server (e.g. for Nemo: https://nemo.sonarqube.org)
        # - SONAR_TOKEN    => token of a user who has the "Execute Analysis" permission on the SQ server

        # And run the analysis
        # It assumes that the project uses Maven and has a POM at the root of the repo
        if [ "$TRAVIS_BRANCH" = "master" ] && [ "$TRAVIS_PULL_REQUEST" = "false" ]; then
                # => This will run a full analysis of the project and push results to the SonarQube server.
                #
                # Analysis is done only on master so that build of branches don't push analyses to the same project and therefore "pollute" the results
                echo "Starting analysis by SonarQube..."
                mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent package sonar:sonar -B -e -V \
                        -Dsonar.host.url=$SONAR_HOST_URL \
                        -Dsonar.organization=$SONAR_ORGANIZATION \
                        -Dsonar.login=$SONAR_TOKEN

        elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN-}" ]; then
                # => This will analyse the PR and display found issues as comments in the PR, but it won't push results to the SonarQube server
                #
                # For security reasons environment variables are not available on the pull requests
                # coming from outside repositories
                # http://docs.travis-ci.com/user/pull-requests/#Security-Restrictions-when-testing-Pull-Requests
                # That's why the analysis does not need to be executed if the variable GITHUB_TOKEN is not defined.
                echo "Starting Pull Request analysis by SonarQube..."
                mvn clean package sonar:sonar -B -e -V \
                        -Dsonar.host.url=$SONAR_HOST_URL \
                        -Dsonar.organization=$SONAR_ORGANIZATION \
                        -Dsonar.login=$SONAR_TOKEN \
                        -Dsonar.analysis.mode=preview \
                        -Dsonar.github.oauth=$GITHUB_TOKEN \
                        -Dsonar.github.repository=$TRAVIS_REPO_SLUG \
                        -Dsonar.github.pullRequest=$TRAVIS_PULL_REQUEST
        fi
        # When neither on master branch nor on a non-external pull request => nothing to do
        ;;
    "debug")
        mvn exec:exec -Dexec.executable=java "-Dexec.args=-classpath %classpath:conf -agentlib:jdwp=transport=dt_socket,server=n,address=127.0.0.1:8000,suspend=y -Dgreeting=\"Hello\" -Ddev=true brs.Burst"
        ;;
    "test")
	mvn -Dtest=$2 test
        ;;
    *)
        usage
        ;;
esac
