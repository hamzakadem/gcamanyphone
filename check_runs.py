import urllib.request
import json
import os
import subprocess

def get_token():
    token = os.environ.get("GITHUB_TOKEN", "")
    if not token:
        try:
            url = subprocess.check_output(["git", "remote", "get-url", "origin"], text=True).strip()
            if "@" in url:
                token = url.split("://")[1].split("@")[0]
        except Exception:
            pass
    return token

TOKEN = get_token()

def make_req(url):
    headers = {'User-Agent': 'Mozilla/5.0'}
    if TOKEN:
        headers['Authorization'] = f'Bearer {TOKEN}'
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode())

def get_latest_run():
    url = 'https://api.github.com/repos/hamzakadem/gcamanyphone/actions/runs?per_page=3'
    try:
        data = make_req(url)
        runs = data.get('workflow_runs', [])
        for r in runs:
            print(f"Run {r['id']}: status={r['status']}, conclusion={r['conclusion']}, commit={r['head_commit']['message'][:60]}")
        return runs[0] if runs else None
    except Exception as e:
        print(f"Error fetching runs: {e}")
        return None

def get_run_jobs(run_id):
    url = f'https://api.github.com/repos/hamzakadem/gcamanyphone/actions/runs/{run_id}/jobs'
    try:
        data = make_req(url)
        for job in data.get('jobs', []):
            print(f"  Job: {job['name']} -> {job['status']} ({job['conclusion']})")
            for step in job.get('steps', []):
                print(f"    {step['name']}: {step['conclusion'] or step['status']}")
    except Exception as e:
        print(f"Error fetching jobs: {e}")

run = get_latest_run()
if run:
    get_run_jobs(run['id'])
