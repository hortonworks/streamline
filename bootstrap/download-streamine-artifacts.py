#!/usr/bin/env python

# Utility for downloading all maven artifacts which are transitive dependencies on Streamline.
#   usage: ./download_streamline_artifacts.py [streamline config file] [maven local repository directory] [output file]
#
# This script has a non-built-in dependency 'pureyaml', so users need to install the dependency via pip or so.
# 'pip install pureyaml'
#
# It is also recommended to create virtualenv and install with activating env.
#
# This script communicates running Streamline instance and binary distribution of Apache Storm.
# Streamline instance is not needed to be run within same node, but binary distribution of Apache Storm 
# must be placed to the same node. 

import sys
import json
import urllib2
import os
import subprocess
import copy
import uuid

# below modules are not built-in, so you need to install via pip or easy_install or so
# recommend to create virtualenv and install with activating env.
try:
    import pureyaml
except ImportError:
    print("ERROR: 'pureyaml' doesn't seem to be installed. Please ensure the module is installed.")
    print("You can install 'pureyaml' via 'pip install pureyaml'.")
    sys.exit(1)

COMPONENT_BUNDLES_KIND_URL_POSTFIX = "/streams/componentbundles"


class ConfigStruct:
    def __init__(self, catalog_root_url, storm_home_dir, maven_repo_url, maven_local_repository_directory):
        self.catalog_root_url = catalog_root_url
        self.storm_home_dir = storm_home_dir
        self.maven_repo_url = maven_repo_url
        self.maven_local_repository_directory = maven_local_repository_directory
        self.http_proxy_url = None
        self.http_proxy_username = None
        self.http_proxy_password = None

    def set_http_proxy(self, proxy_url, proxy_username = None, proxy_password = None):
        self.http_proxy_url = proxy_url
        self.http_proxy_username = proxy_username
        self.http_proxy_password = proxy_password

    def get_storm_submit_tool_classpath(self):
        storm_submit_tool_dir = os.path.join(self.storm_home_dir, "external", "storm-submit-tools")
        storm_submit_tool_classpath = os.path.join(storm_submit_tool_dir, "*")
        return storm_submit_tool_classpath

    def get_component_bundles_kind_url(self):
        return self.catalog_root_url + COMPONENT_BUNDLES_KIND_URL_POSTFIX

    def get_component_bundles_url(self, component):
        return self.get_component_bundles_kind_url() + "/" + component

    def build_storm_submit_tool_command(self, artifacts):
        submit_tool_cmd_list = ["java", "-cp", self.get_storm_submit_tool_classpath()]
        submit_tool_cmd_list.extend(["org.apache.storm.submit.command.DependencyResolverMain"])
        submit_tool_cmd_list.extend(["--artifactRepositories", self.maven_repo_url])
        submit_tool_cmd_list.extend(["--artifacts", artifacts])
        submit_tool_cmd_list.extend(["--mavenLocalRepositoryDirectory", self.maven_local_repository_directory])
        return submit_tool_cmd_list


def load_config(config_file, maven_local_repository_directory):
    with open(config_file, 'r') as fr:
        content = fr.read()
        config = pureyaml.load(content)

        catalog_root_url = config['catalogRootUrl']
        streams_config = filter(lambda x: x['name'] == 'streams', config['modules'])[0]['config']
        storm_home_dir = streams_config['stormHomeDir']
        maven_repo_url = streams_config['mavenRepoUrl']

        http_proxy_url = None
        http_proxy_username = None
        http_proxy_password = None

        if 'httpProxyUrl' in streams_config:
            http_proxy_url = streams_config['httpProxyUrl']

        if 'httpProxyUsername' in streams_config:
            http_proxy_username = streams_config['httpProxyUsername']

        if 'httpProxyPassword' in streams_config:
            http_proxy_password = streams_config['httpProxyPassword']

        config_struct = ConfigStruct(catalog_root_url, storm_home_dir, maven_repo_url, maven_local_repository_directory)
        if http_proxy_url is not None:
            config_struct(http_proxy_url, http_proxy_username, http_proxy_password)

        return config_struct


def load_component_bundles_kind(config_struct):
    url = config_struct.get_component_bundles_kind_url()
    content = urllib2.urlopen(url)
    resp_json = json.load(content)
    return resp_json['entities']


def collect_maven_deps_list(config_struct, component_bundles_kind):
    maven_deps_list = []
    for component in component_bundles_kind:
        component_bundles_url = config_struct.get_component_bundles_url(component)
        content = urllib2.urlopen(component_bundles_url)
        resp_json = json.load(content)
        entities_maven_deps = filter(lambda x: 'mavenDeps' in x and x['mavenDeps'] != None, resp_json['entities'])
        maven_deps_list.extend(map(lambda x: x['mavenDeps'], entities_maven_deps))
    return maven_deps_list


def write_downloaded_information_to_output_file(output_file, downloaded_artifacts):
    with open(output_file, 'w') as fw:
        for artifact_info, downloaded_location in downloaded_artifacts.iteritems():
            fw.write("%s\t%s\n" % (artifact_info, downloaded_location))


def main():
    if len(sys.argv) < 4:
        print("USAGE: [python] %s [streamline config file] [maven local repository directory] [output file]" % sys.argv[0])
        sys.exit(1)

    config_file = sys.argv[1]
    maven_local_repository_directory = sys.argv[2]
    output_file = sys.argv[3]

    print("> CONFIG FILE: %s" % config_file)
    print("> MAVEN LOCAL REPOSITORY DIRECTORY: %s" % maven_local_repository_directory)
    print("> OUTPUT FILE: %s" % output_file)

    config_struct = load_config(config_file, maven_local_repository_directory)

    print("================ CONFIG ==================")
    print("CATALOG ROOT URL: %s" % config_struct.catalog_root_url)
    print("STORM HOME DIR: %s" % config_struct.storm_home_dir)
    print("MAVEN REPOSITORY URL: %s" % config_struct.maven_repo_url)
    print("HTTP PROXY URL: %s" % config_struct.http_proxy_url)
    print("HTTP PROXY USERNAME: %s" % config_struct.http_proxy_username)
    print("HTTP PROXY PASSWORD: %s" % config_struct.http_proxy_password)
    print("STORM SUBMIT TOOL CLASSPATH: %s" % config_struct.get_storm_submit_tool_classpath())

    component_bundles_kind = load_component_bundles_kind(config_struct)

    maven_deps_list = collect_maven_deps_list(config_struct, component_bundles_kind)

    print("============== COLLECTED Maven Dependencies ==============")
    for maven_deps in maven_deps_list:
        print(maven_deps)

    downloaded_artifacts = {}

    for maven_deps in maven_deps_list:
        submit_tool_cmd = config_struct.build_storm_submit_tool_command(maven_deps)
        print("============== Running COMMAND: %s" % submit_tool_cmd)
        p = subprocess.Popen(submit_tool_cmd, stdout=subprocess.PIPE)
        stdout, errors = p.communicate()
        print("============== Return value of SUBMIT TOOL: %s" % p.returncode)
        if p.returncode != 0:
            raise RuntimeError("dependency handler returns non-zero code: code<%s> syserr<%s>" % (p.returncode, errors))
        if not isinstance(stdout, str):
            stdout = stdout.decode('utf-8')
        ret_json = json.loads(stdout)
        downloaded_artifacts.update(ret_json)

    print("============== Writing downloaded information to file: %s" % output_file)
    write_downloaded_information_to_output_file(output_file, downloaded_artifacts)

    print("DONE!")

    print("Please follow below steps:")
    print("1. archive maven local repository directory which downloaded artifacts are placed.")
    print("2. send archived file to the machines which streamline is installed and don't have access to internet.")
    print("3. extract file to specific directory which streamline and storm account can read and write directory and jars.")
    print("4. set configuration for maven local repository directory to the place from 3. in streamline configuration.")
    print("5. restart streamline to take effect.")


if __name__ == "__main__":
    main()
