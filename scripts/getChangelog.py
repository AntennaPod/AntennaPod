#!/usr/bin/env python3

# TODO:
# Make it possible to run the script through workflow file, completely headless, with only base_ref and head_ref as input
# Make it possible to run the part of the script that creates a list of PRs that need review replies in a workflow file (which posts a comment for each of the relevant PRs/issues when creating a release)
# i.e. define two headless states: "get_changelog_CSV" and "get_PRs_that_need_review_replies"
# However, the former should already be possibe by running the script with only base_ref and head_ref as input

try:
  from graphqlclient import GraphQLClient
  import json
  import time
  from datetime import datetime
  import csv
  from collections import OrderedDict
  import sys
  import os
  import getpass
  import threading
  import urllib.error
  import textwrap
except ModuleNotFoundError as e:
  print(f"The '{e.name}' module is not installed. Please install it using 'pip install {e.name}' and try again.")
  exit()

# Define variables
owner = os.getenv('OWNER', "AntennaPod") # The owner (organisation or user) of the repository
repo = os.getenv('REPO', "AntennaPod") # The repository name
token = os.getenv('GITHUB_API_TOKEN') # The GitHub API token
base_ref = os.getenv('BASE') # The base reference (release code or branch); point of reference
head_ref = os.getenv('HEAD') # The head reference (release code or branch); environment containing the changes
needs_review_reply_label = os.getenv('NEED_REVIEW_REPLY_LABEL', "Needs: Review reply") # The name or ID of the label that indicates that we should reply to a (Google Play) review when the issue or PR that has this label iss addressed or released
client = GraphQLClient('https://api.github.com/graphql')

# Function: Handle exceptions
def handle_exception(error_message: str, data: dict, error_introduction="Error"):
  print(f"\n{error_introduction}: {str(error_message)}")
  if data:
    print("JSON data:")
    print(json.dumps(data, indent=2))

# Function: Display processing animation
animation_state = {"text": "Loading", "stop": False}
def display_processing_animation(state):
  print()
  characters = ["⣷", "⣯", "⣟", "⡿", "⢿", "⣻", "⣽", "⣾"]
  while not state['stop']:
    for char in characters:
      print(f"\r{char} {state['text']}...                    ", end="", flush=True)
      time.sleep(0.5)
  print("\r                                       ", end="\r", flush=True)

# Function: Get list of PRs for a given commit
# Note: this might be unnecessary in case commits can only have one PR
def get_associated_prs(commitid):
  """
  Called in case the number of PRs linked to a commit exceeds the maximum number of PRs that the script will fetch per commit.
  """
  global animation_text
  animation_text = "Get PRs linked to commit"
  query = '''
  query ($cursor: String, $owner: String!, $repo: String!, $commitid: GitObjectID!) { 
    repository(name: $repo, owner: $owner) { 
      object(oid: $commitid) {
        ... on Commit {
          associatedPullRequests(first: 100, after: $cursor) {
            pageInfo {
              endCursor
              hasNextPage
            }
            nodes {
              number
            }
          }
        }
      }
    }
  }
  '''
  # Variable definition for GraphQL Explorer:
  # {"commitid": "863d4c3b611df83389d82958114bfd2d1204e457", "cursor": null, "owner": "AntennaPod", "repo": "AntennaPod"}
  has_next_pr_page = True
  cursor = None
  pr_numbers = []  # Create PR list
  while has_next_pr_page:
    variables = {"cursor": cursor, "owner": owner, "repo": repo, "commitid": commitid }
    result = client.execute(query, variables)
    data = json.loads(result)        
    pr_numbers.extend([pr['number'] for pr in data['data']['repository']['commit']['associatedPullRequests']['nodes']])
    page_info = data['data']['repository']['commit']['associatedPullRequests']['pageInfo']
    has_next_pr_page = page_info['hasNextPage']
    cursor = page_info['endCursor']
    
    time.sleep(1)  # To prevent hitting rate limits
    return pr_numbers

try: # Catch KeyboardInterrupt
  # Define animation thread to avoid errors
  animation_thread = None

  # Define token
  print(f"Hello, welcome to the {owner} PR list generator!")
  time.sleep(0.5)
  print("First, please enter your GitHub API token.")
  print("If you don't have one yet, create it at https://github.com/settings/tokens")
  def get_token():
    global token
    data = None
    while True:
      if not token:
        token = getpass.getpass(prompt='Token: ')
      try:
        client.inject_token('Bearer ' + token)
        query = '''
        query {
          viewer {
            login
          }
        }
        '''
        response = client.execute(query) # This prints the response in case of error, despite any try/except blocks
        data = json.loads(response)
        if 'login' in data['data']['viewer']:
          print(f"Grand, thank you @{data['data']['viewer']['login']}!")
          print("Do you want learn how to save this token for future use? [y/N]")
          save = input()
          if save.lower() == "y":
            print(textwrap.dedent(f"""
            To save this token for future use, you need to set it as an environment variable on your system. Here's how you can do it:
            
            If you're using bash or zsh, you can add the following line to your shell profile file (.bashrc, .bash_profile or .zshrc):
            export GITHUB_API_TOKEN='{token}'
            
            If you're using Fish shell, you can add the following line to your config.fish file:
            set -x GITHUB_API_TOKEN '{token}'
            
            After adding this line, you'll need to restart your terminal or run 'source ~/.bashrc' (or the appropriate file for your shell) for the changes to take effect.
            
            On Windows, you can set environment variables through the System Properties. Here's how:
            1. Right-click on Computer on the desktop or in the Start menu.
            2. Choose Properties.
            3. Click on Advanced system settings.
            4. Click on Environment Variables.
            5. Click on New under the User or System variables sections.
            6. Enter 'GITHUB_API_TOKEN' as the variable name and '{token}' as the variable value.
            7. Click OK in all windows.
            
            Please note that setting environment variables this way will make them available in all future terminal sessions and scripts. Use with caution.
            """))
          else:
            print("Ok, moving on.")
          break
      except urllib.error.HTTPError as error_message:
        if error_message.code == 401:
          handle_exception(Exception("Invalid GitHub API token, please try again."), data)
          token = ""
        else:
          handle_exception(Exception("Issue executing GraphQL query"), data)
          token = ""
      except Exception as error_message:
        handle_exception(error_message, data)
        token = ""
    return token

  if not token:
    token = get_token()
  # Assuming authentication is successful, we will no longer check for/catch authentication errors.

  # Define base_ref
  if not base_ref:
    print()
    print("Now, what should be our point of reference?")
    print("Please enter a release code or branch")
    print("[default: latest (in other words: previous) GitHub release]")
    base_ref = input('Base: ')

  if base_ref == "latest" or not base_ref:
    query = '''
    query ($owner: String!, $repo: String!) {
      repository(owner: $owner, name: $repo) {
        latestRelease {
          tagName
        }
      }
    }
    '''
    while True:
      try:
        variables = {"owner": owner, "repo": repo}
        response = client.execute(query, variables)
        data = json.loads(response)
        base_ref = data['data']['repository']['latestRelease']['tagName']
        print("\nOkido, let's get the latest release (" + base_ref + ")!")
        break
      except Exception as e:
        handle_exception(e, data)
        print("Does your token have enough permissions?")
        sys.exit(1)
  else:
    query = '''
    query ($owner: String!, $repo: String!, $ref: String!) {
      repository(owner: $owner, name: $repo) {
        ref(qualifiedName:"$ref") {
          name
        }
      }
    }
    '''
    while True:
      try:
        variables = {"owner": owner, "repo": repo, "ref": base_ref}
        response = client.execute(query, variables)
        data = json.loads(response)
        if data['data']['repository']['ref'] == None:
          print("\nError: Invalid release code or branch.")
          print("Please try again.")
          base_ref = input('Base: ')
          continue
        base_ref = data['data']['repository']['ref']['name']
        print(f"\nNoted, {base_ref} it is.")
        break
      except Exception as e:
        handle_exception(e, data)
        print("Does your token have enough permissions?")
        sys.exit(1)

  # Define head_ref
  if not head_ref:
    print("\nThen, from which environment would you like to see the changes (the head)?")
    print("Please enter a release code or branch")
    print("[default: 'master']")
    head_ref = input('Head: ')

  if head_ref == "master" or not head_ref:
      print("\nRighty, master it is!")
  if not head_ref:
    head_ref = "master"
  else:
    query = '''
    query ($owner: String!, $repo: String!, $ref: String!) {
      repository(owner: $owner, name: $repo) {
        ref(qualifiedName: $ref) {
          id
          name
        }
      }
    }
    '''
    while True:
      try:
        variables = {"owner": owner, "repo": repo, "ref": head_ref}
        response = client.execute(query, variables)
        data = json.loads(response)
        if data['data']['repository']['ref'] == None:
          print("\nError: Invalid release code or branch.")
          print("Please try again.")
          head_ref = input('Head: ')
          continue
        head_ref = data['data']['repository']['ref']['name']
        print(f"\nNoted, {head_ref} it is.")
        break
      except Exception as e:
          handle_exception(e, data)
          print("Does your token have enough permissions?")
          sys.exit(1)

  # Start the animation in a separate thread
  animation_thread = threading.Thread(target=display_processing_animation, args=(animation_state,))
  animation_thread.start()

  # Get list of commits & associated PRs, comparing base & head
  animation_state['text'] = "Get list of commits & PRs"
  query = '''
  query($cursor: String, $owner: String!, $repo: String!, $baseRef: String!, $headRef: String!, $maxPRs: Int!) { 
    repository(name: $repo, owner: $owner) { 
      ref(qualifiedName: $baseRef) {
        compare(headRef: $headRef) {
          commits(first: 100, after: $cursor) {
            pageInfo {
              endCursor
              hasNextPage
            }
            nodes {
              oid
              associatedPullRequests(first: $maxPRs) {
                totalCount
                nodes {
                  number
                }
              }
            }
          }
        }
      }
    }
  }
  '''
  # Variable definition for GraphQL Explorer:
  # {"cursor": null, "owner": "AntennaPod", "repo": "AntennaPod", "baseRef": "master", "headRef": "develop", "maxPRs": 5}

  has_next_page = True
  cursor = None
  commits = []  # Create commit list
  max_associatedPRs = 5 # The maximum number of pull requests that the script will fetch per commit
  while has_next_page:
      variables = {"cursor": cursor, "owner": owner, "repo": repo, "baseRef": base_ref, "headRef": head_ref, "maxPRs": max_associatedPRs}
      result = client.execute(query, variables)
      data = json.loads(result)
      
      commit_data = data['data']['repository']['ref']['compare']['commits']['nodes']
      for commit in commit_data:
          if commit['associatedPullRequests']['totalCount'] > max_associatedPRs: # Request a list of PRs for the commit, if there are many.
            commitid = commit['oid']
            pr_numbers = get_associated_prs(commitid)
          else:
            pr_numbers = [pr['number'] for pr in commit['associatedPullRequests']['nodes']]
          
          commits.append({  # Store commit information in list
            'sha': commit['oid'],
            'pr_count': commit['associatedPullRequests']['totalCount'],
            'pr_numbers': pr_numbers,
          })
          
      page_info = data['data']['repository']['ref']['compare']['commits']['pageInfo']
      has_next_page = page_info['hasNextPage']
      cursor = page_info['endCursor']
      
      time.sleep(1)  # To prevent hitting rate limits

  # Create set of unique PRs from the list of commit dictionaries
  animation_state['text'] = "Identify unique PRs"
  unique_pr_numbers = set()
  for commit in commits:
      for pr_number in commit['pr_numbers']:
          unique_pr_numbers.add(pr_number)

  # Create a list of dictionaries with PR metadata for in CSV file
  animation_state['text'] = "Get PR metadata"
  ## Combined GraphQL call: get relevant PRs and the issues they close
  query = f'''
  query ($owner: String!, $repo: String!) {{
    repository (name: $repo, owner: $owner) {{
  '''

  for n, pr_number in enumerate(unique_pr_numbers):
      query += f'''
        pr{n}:pullRequest (number: {pr_number}) {{
          url
          title
          author {{
            login
          }}
          mergedAt
          labels (first: 10) {{
            totalCount
            nodes {{
              id
              name
            }}
          }}
          closingIssuesReferences(first: 10) {{
            totalCount
            nodes {{
              number
              title
              labels(first: 10) {{
                totalCount
                nodes {{
                  id
                  name
                }}
              }}
            }}
          }}
        }}
        '''

  query += f'''
    }}
  }}
  '''
  # Variable definition for GraphQL Explorer:
  # {"prnumber": 7053, "owner": "AntennaPod", "repo": "AntennaPod"}

  ## Submit call
  variables = {"owner": owner, "repo": repo}
  result = client.execute(query, variables)
  data = json.loads(result)
      
  ## Parse response and save PR data
  animation_state['text'] = "Parse PR metadata"
  prs_for_csv = []
  issues_PRs_needing_review_replies = []

  for n, pr_number in enumerate(unique_pr_numbers):
      prdata = data['data']['repository'][f'pr{n}']
      
      # Create string with related issues
      maximum_hit = False
      related_issue_numbers = [relatedIssue['number'] for relatedIssue in prdata['closingIssuesReferences']['nodes']]
      related_issues_string = ', '.join(map(str, related_issue_numbers))
      if prdata['closingIssuesReferences']['totalCount'] > 10:
        related_issues_string += " and more"
      
      # Create string with labels of the PR and its associated issues
      unique_labels = set()
      maximum_hit = False
      if prdata['labels']['totalCount'] > 10:
        maximum_hit = True
      for label in prdata['labels']['nodes']:
        unique_labels.add(label['name'])
      for relatedIssue in prdata['closingIssuesReferences']['nodes']:
        if relatedIssue['labels']['totalCount'] > 10:
          maximum_hit = True
        for label in relatedIssue['labels']['nodes']:
          unique_labels.add(label['name'])
          
      unique_labels_list = list(unique_labels)
      unique_labels_string = ', '.join(unique_labels_list)
      if maximum_hit:
        unique_labels_string += " and more (probably)"
      
      # Create string with issue & PR number(s) that need review replies,
      # and store total list of PRs that need review replies for processing in workflow
      if needs_review_reply_label:
        numbers = []
        maximum_hit = False
        if any(label['id'] == 'needs_review_reply_label' or label['name'] == 'needs_review_reply_label' for label in prdata['labels']['nodes']):
          numbers.append(pr_number)
        if prdata['closingIssuesReferences']['totalCount'] > 10 or prdata['labels']['totalCount'] > 10:
          maximum_hit = True
        for relatedIssue in prdata['closingIssuesReferences']['nodes']:
          if any(label['id'] == 'needs_review_reply_label' for label in relatedIssue['labels']['nodes']):
            numbers.append(relatedIssue['number'])
          if relatedIssue['labels']['totalCount'] > 10:
            maximum_hit = True
        numbers_str = ', '.join(map(str, numbers))
        issues_PRs_needing_review_replies.extend(numbers)
        if maximum_hit:
          numbers_str += " and more, possibly"
          # TODO: move this whole block (which checks the linked issues of a PR and if they have the relevant label) to a separate function, which deals with pagination
        if numbers_str:
          needs_review_reply_string = f"Yes ({numbers_str})"
        else:
          needs_review_reply_string = "No"
      
      # Store pr information in list
      prs_for_csv.append({
        'mergedAt': prdata['mergedAt'],
        'URL': prdata['url'],
        'title': prdata['title'],
        'author': prdata['author']['login'],
        'relatedIssues': related_issues_string,
        'labels': unique_labels_string,
      })
      if needs_review_reply_label:
        prs_for_csv[-1]['needsReviewReplies'] = needs_review_reply_string

  # Create a list of dictionaries with commits for in the CSV file
  animation_state['text'] = "Clean up commit list"
  ## Filter list with commit dictionaries so only ones without any associated PRs are left
  commits = [commit for commit in commits if commit['pr_count'] == 0]

  animation_state['text'] = "Get commit metadata"

  ### Loop through commits to construct GraphQL query
  query = f'''
  query ($owner: String!, $repo: String!) {{
    repository (name: $repo, owner: $owner) {{
  '''

  for n, commit in enumerate(commits):
      query += f'''
          commit{n}:object(oid: "{commit['sha']}") {{
            ... on Commit {{
              message
              committedDate
              url
              authors(first:3) {{
                totalCount
                nodes {{
                  user {{
                    login
                  }}
                }}
              }}
            }}
          }}
  '''

  query += f'''
    }}
  }}
  '''
  # Variable definition for GraphQL Explorer:
  # {"sha": "863d4c3b611df83389d82958114bfd2d1204e457", "owner": "AntennaPod", "repo": "AntennaPod"}

  ## Submit call
  variables = {"owner": owner, "repo": repo}
  result = client.execute(query, variables)
  data = json.loads(result)

  ## Parse response and add commit metadata to the list of commit dictionaries
  animation_state['text'] = "Parse commit metadata"
  for n, commit in enumerate(commits):
    commit_data = data['data']['repository'][f'commit{n}']
    
    # Create string with authors
    authors = [author['user']['login'] for author in commit_data['authors']['nodes']]
    authors_string = ', '.join(authors)
      
    commit['committedDate'] = commit_data['committedDate']
    commit['URL'] = commit_data['url']
    commit['title'] = commit_data['message']
    commit['author'] = authors_string

  # Combine commit & PR lists & sort by mergedAt (for PRs)/ committedDate (for commits)
  animation_state['text'] = "Combine and sort PR and commit lists"
  commits = [{**commit, 'datetime': datetime.strptime(commit['committedDate'], '%Y-%m-%dT%H:%M:%SZ'), 'entitytype': 'commit'} for commit in commits]
  prs_for_csv = [{**pr, 'datetime': datetime.strptime(pr['mergedAt'], '%Y-%m-%dT%H:%M:%SZ'), 'entitytype': 'pr'} for pr in prs_for_csv]
  for commit in commits:
    del commit['committedDate']
  for pr in prs_for_csv:
    del pr['mergedAt']
  combined = commits + prs_for_csv
  combined.sort(key=lambda x: x['datetime'])
  for row in combined:    
      row.pop('sha', None)
      row.pop('pr_count', None)
      row.pop('pr_numbers', None)
      row['empty1'] = ''
      row['empty2'] = ''

  # Define your fieldnames and their human-readable counterparts
  animation_state['text'] = "Save changelog as CSV"
  fields = [
    ('entitytype', 'Entity'),
    ('datetime', 'Merge/Committed date'),
    ('URL', 'URL'),
    ('title', 'Title'),
    ('author', 'Author(s)'),
    ('empty1', 'Type'),
    ('empty2', 'Functionality group'),
    ('relatedIssues', 'Related issue(s)'),
    ('labels', 'Related label(s)'),
    ]
  if needs_review_reply_label:
    fields.append(('needsReviewReplies', 'Needs review replies?'))

  # Create an OrderedDict from the fields
  fieldnames = OrderedDict(fields)
  header = dict(zip(fieldnames.keys(), fieldnames.values()))

  # Set filename & export to GH Actions environment
  filename = f'{base_ref} - {head_ref} changelog.csv'
  env_file = os.getenv('GITHUB_ENV')
  if env_file:
    os.system(f'echo "::set-output name=filename::{filename}"')

  with open(f'{filename}', 'w', newline='') as outputFile:
      # Use the OrderedDict as the fieldnames argument
      writer = csv.DictWriter(outputFile, fieldnames=fieldnames)
      writer.writerow(header)
      writer.writerows(combined)  # Writes all the dictionaries in the list to the CSV

  # Export list of PRs that need review replies to GH Actions environment
  if env_file:
    needing_review_replies_string = ' '.join(map(str, issues_PRs_needing_review_replies))
    os.system(f'echo "::set-output name=issues::{needing_review_replies_string}"')

  # Stop the animation
  animation_state['stop'] = True
  animation_thread.join()
  print("✅ The changelog has been saved as a CSV file.")
  print(f"📂 The file is named '{filename}'.")
except KeyboardInterrupt:
  animation_state['text'] = "Ending"
  if animation_thread:
    animation_state['stop'] = True
    try:
      animation_thread.join()
    except KeyboardInterrupt:
      animation_state['text'] = "Still wrapping up"
  if filename and os.path.exists(filename):
    os.remove(filename)
    print("  The requested changelog file was deleted.")
  print("\n⚠️ The script was interrupted by the user.")