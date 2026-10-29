import urllib.request
import json
import time

def get_latest_run():
    url = 'https://api.github.com/repos/hamzakadem/gcamanyphone/actions/runs?per_page=3'
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode())
        runs = data.get('workflow_runs', [])
        for r in runs:
            print(f"Run {r['id']}: status={r['status']}, conclusion={r['conclusion']}, commit={r['head_commit']['message'][:60]}")
        return runs[0] if runs else None

def get_run_jobs(run_id):
    url = f'https://api.github.com/repos/hamzakadem/gcamanyphone/actions/runs/{run_id}/jobs'
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode())
        for job in data.get('jobs', []):
            print(f"  Job: {job['name']} -> {job['status']} ({job['conclusion']})")
            for step in job.get('steps', []):
                print(f"    {step['name']}: {step['conclusion'] or step['status']}")

run = get_latest_run()
if run:
    get_run_jobs(run['id'])
