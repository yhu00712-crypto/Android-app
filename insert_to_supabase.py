import urllib.request
import json
import sys

def insert_jobs():
    base_url = 'https://wnvvkoboeroeetixszku.supabase.co/rest/v1/jobs'
    key = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndudnZrb2JvZXJvZWV0aXhzemt1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA2NjYxMDIsImV4cCI6MjEwNjI0MjEwMn0.lyiYgWmCSo9dZEdHWqroognO53MmlnSMap7m6baN5Cg'
    headers = {
        'apikey': key,
        'Authorization': f'Bearer {key}',
        'Content-Type': 'application/json',
        'Prefer': 'return=minimal'
    }

    try:
        with open('jobs.json', 'r', encoding='utf-8') as f:
            jobs = json.load(f)
    except Exception as e:
        print(f"Failed to read jobs.json: {e}")
        return

    print(f"Total jobs to insert: {len(jobs)}")
    payload = json.dumps(jobs).encode('utf-8')

    req = urllib.request.Request(base_url, data=payload, headers=headers, method='POST')
    try:
        with urllib.request.urlopen(req) as resp:
            print(f"Successfully inserted all {len(jobs)} jobs into Supabase! (HTTP {resp.status})")
    except urllib.error.HTTPError as e:
        err_msg = e.read().decode('utf-8')
        print(f"HTTPError {e.code}: {err_msg}")
    except Exception as e:
        print(f"Error: {e}")

if __name__ == '__main__':
    insert_jobs()
