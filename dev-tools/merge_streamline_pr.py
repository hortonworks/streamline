#!/usr/bin/env python

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Utility for creating well-formed pull request merges and pushing them to Streamline.
#   usage: ./merge_streamline_pr.py    (see config env vars below)
#
# This script is inspired by Spark merge script and also borrow some codes from Kafka.
#
# This utility assumes you already have a local Streamline git folder and that you
# have added remotes corresponding to both (i) the pull repository (which pull requests are available)
# and (ii) the push repository.

import json
import os
import subprocess
import sys
import urllib2

# Location of your Streamline git development area
STREAMLINE_HOME = os.environ.get("STREAMLINE_HOME", os.getcwd())
# Remote name which points to pull repository
PR_REMOTE_NAME = os.environ.get("PR_REMOTE_NAME", "pull-repo")
# Remote name which points to push repository
PUSH_REMOTE_NAME = os.environ.get("PUSH_REMOTE_NAME", "push-repo")
# OAuth key used for issuing requests against the GitHub API. If this is not defined, then requests
# will be unauthenticated. You should only need to configure this if you find yourself regularly
# exceeding your IP's unauthenticated request rate limit. You can create an OAuth key at
# https://github.com/settings/tokens. This script only requires the "public_repo" scope.
GITHUB_OAUTH_KEY = os.environ.get("GITHUB_OAUTH_KEY")


GITHUB_BASE = "https://github.com/hortonworks/streamline/pull"
GITHUB_API_BASE = "https://api.github.com/repos/hortonworks/streamline"
# Prefix added to temporary branches
BRANCH_PREFIX = "PR_TOOL"


def get_json(url):
    try:
        request = urllib2.Request(url)
        if GITHUB_OAUTH_KEY:
            request.add_header('Authorization', 'token %s' % GITHUB_OAUTH_KEY)
        return json.load(urllib2.urlopen(request))
    except urllib2.HTTPError as e:
        if "X-RateLimit-Remaining" in e.headers and e.headers["X-RateLimit-Remaining"] == '0':
            message = "Exceeded the GitHub API rate limit; see the instructions in " +\
                  "dev/merge_streamline_pr.py to configure an OAuth token for making authenticated " +\
                  "GitHub requests."
        else:
            message = "Unable to fetch URL, exiting: %s" % url
        fail(message)


def fail(msg):
    print(msg)
    clean_up()
    sys.exit(-1)


def run_cmd(cmd):
    print(cmd)
    try:
        if isinstance(cmd, list):
            return subprocess.check_output(cmd)
        else:
            return subprocess.check_output(cmd.split(" "))
    except subprocess.CalledProcessError as e:
        print("CallProcessError occurred. More information for process is below...")
        print("Output - %s" % e.output)
        print("Return code - %d" % e.returncode)
        raise


def continue_maybe(prompt):
    result = raw_input("\n%s (y/n): " % prompt)
    if result.lower() != "y":
        fail("Okay, exiting")


def clean_up():
    print("Restoring head pointer to %s" % original_head)
    run_cmd("git checkout %s" % original_head)

    branches = run_cmd("git branch").replace(" ", "").split("\n")

    for branch in filter(lambda x: x.startswith(BRANCH_PREFIX), branches):
        print("Deleting local branch %s" % branch)
        run_cmd("git branch -D %s" % branch)


# merge the requested PR and return the merge hash
def merge_pr(pr_num, target_ref, title, body, pr_repo_desc):
    pr_branch_name = "%s_MERGE_PR_%s" % (BRANCH_PREFIX, pr_num)
    target_branch_name = "%s_MERGE_PR_%s_%s" % (BRANCH_PREFIX, pr_num, target_ref.upper())
    run_cmd("git fetch %s pull/%s/head:%s" % (PR_REMOTE_NAME, pr_num, pr_branch_name))
    run_cmd("git fetch %s %s:%s" % (PUSH_REMOTE_NAME, target_ref, target_branch_name))
    run_cmd("git checkout %s" % target_branch_name)

    commits = run_cmd(['git', 'log', 'HEAD..%s' % pr_branch_name,
                       '--pretty=format:%h [%an] %s']).split("\n")
    commit_authors = run_cmd(['git', 'log', 'HEAD..%s' % pr_branch_name,
                              '--pretty=format:%an <%ae>']).split("\n")
    distinct_authors = sorted(set(commit_authors),
                              key=lambda x: commit_authors.count(x), reverse=True)

    print("Information of commits: %s" % (commits,))

    if len(distinct_authors) > 1:
        fail("We don't allow squashing commits which have multiple authors. You need to handle the merge manually. authors: %s" % (distinct_authors,))

    primary_author = distinct_authors[0]

    had_conflicts = False
    try:
        run_cmd(['git', 'merge', pr_branch_name, '--squash'])
    except Exception as e:
        msg = "Error merging: %s\nWould you like to manually fix-up this merge?" % e
        continue_maybe(msg)
        msg = "Okay, please fix any conflicts and 'git add' conflicting files... Finished?"
        continue_maybe(msg)
        had_conflicts = True

    merge_message_flags = []

    merge_message_flags += ["-m", title]
    if body is not None:
        # We remove @ symbols from the body to avoid triggering e-mails
        # to people every time someone creates a public fork of Streamline.
        merge_message_flags += ["-m", body.replace("@", "")]

    merge_message_flags += ["-m", "Author: %s" % primary_author]

    if had_conflicts:
        committer_name = run_cmd("git config --get user.name").strip()
        committer_email = run_cmd("git config --get user.email").strip()
        message = "This patch had conflicts when merged, resolved by\nCommitter: %s <%s>" % (
            committer_name, committer_email)
        merge_message_flags += ["-m", message]

    if len(commits) > 1:
        result = raw_input("List pull request commits in squashed commit message? (y/n) [n]: ")
        if result.lower() == "y":
            should_list_commits = True
        else:
            should_list_commits = False
    else:
        should_list_commits = False

    # The string "Closes #%s" string is required for GitHub to correctly close the PR
    close_line = "Closes #%s from %s" % (pr_num, pr_repo_desc)
    if should_list_commits:
        close_line += " and squashes the following commits:"
    merge_message_flags += ["-m", close_line]

    if should_list_commits:
        merge_message_flags += ["-m", "\n".join(commits)]

    run_cmd(['git', 'commit', '--author="%s"' % primary_author] + merge_message_flags)

    result = raw_input("Merge complete (local ref %s). Push to %s? (y/n)")
    if result.lower() != "y":
        result = raw_input("Exiting. Do you want to keep the current state? (expert only) (y/n)")
        if result.lower() != "y":
            fail("Okay, exiting")
        else:
            print("Okay, exiting without cleaning up.")
            sys.exit(0)

    try:
        run_cmd('git push %s %s:%s' % (PUSH_REMOTE_NAME, target_branch_name, target_ref))
    except Exception as e:
        clean_up()
        fail("Exception while pushing: %s" % e)

    merge_hash = run_cmd("git rev-parse %s" % target_branch_name)[:8]
    clean_up()
    print("Pull request #%s merged!" % pr_num)
    print("Merge hash: %s" % merge_hash)
    return merge_hash


def get_current_ref():
    ref = run_cmd("git rev-parse --abbrev-ref HEAD").strip()
    if ref == 'HEAD':
        # The current ref is a detached HEAD, so grab its SHA.
        return run_cmd("git rev-parse HEAD").strip()
    else:
        return ref


def get_remotes():
    remotes_output = run_cmd("git remote -v").strip().split("\n")
    return set(map(lambda x: x.split("\t")[0], remotes_output))


def main():
    global original_head

    os.chdir(STREAMLINE_HOME)
    original_head = get_current_ref()
    remotes = get_remotes()

    if not PR_REMOTE_NAME in remotes:
      fail("Remote for pull request [%s] not registered" % PR_REMOTE_NAME)

    if not PUSH_REMOTE_NAME in remotes:
      fail("Remote for push [%s] not registered" % PUSH_REMOTE_NAME)

    pr_num = raw_input("Which pull request would you like to merge? (e.g. 34): ")
    pr = get_json("%s/pulls/%s" % (GITHUB_API_BASE, pr_num))

    url = pr["url"]

    print("The title of PR: %s" % pr["title"])

    if not pr["title"].startswith("ISSUE-"):
        print("The title of PR doesn't conform to the Streamline rule: doesn't start with 'ISSUE-'")
        continue_maybe("Continue merging?")

        result = raw_input("Do you want to modify the title before continue? (y/n): ")
        if result.lower() == "y":
            title_str = raw_input("New title: ")
            title = title_str.strip()
        else:
            print("OK. Will use PR's title.")
            title = pr["title"]
    else:
        title = pr["title"]

    body = pr["body"]

    if not body or len(body.strip()) <= 0:
        print("WARN: The body of PR doesn't have any content which should have information of PR.")
        print("If you continue merging, commit title is available but commit message may be empty.")
        continue_maybe("Do you want to continue?")

    target_ref = pr["base"]["ref"]
    user_login = pr["user"]["login"]
    base_ref = pr["head"]["ref"]
    pr_repo_desc = "%s/%s" % (user_login, base_ref)

    if pr["state"] != "open":
        fail("The state of PR is not 'open'. We don't want to deal with closed PR.")

    if not bool(pr["mergeable"]):
        msg = "Pull request %s is not mergeable in its current form.\n" % pr_num + \
            "Continue? (experts only!)"
        continue_maybe(msg)

    print("\n=== Pull Request #%s ===" % pr_num)
    print("title\t%s\nsource\t%s\ntarget\t%s\nurl\t%s" %
          (title, pr_repo_desc, target_ref, url))
    continue_maybe("Proceed with merging pull request #%s?" % pr_num)

    merge_pr(pr_num, target_ref, title, body, pr_repo_desc)


if __name__ == "__main__":
    try:
        main()
    except SystemExit:
        # don't clean_up() while receiving SystemExit: the situation might be normal exit
        pass
    except:
        clean_up()
        raise
