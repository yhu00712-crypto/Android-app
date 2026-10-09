import urllib.request
import json
import csv

base_url = 'https://wnvvkoboeroeetixszku.supabase.co/rest/v1/recruiting_jobs'
key = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndudnZrb2JvZXJvZWV0aXhzemt1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTA2NjYxMDIsImV4cCI6MjEwNjI0MjEwMn0.lyiYgWmCSo9dZEdHWqroognO53MmlnSMap7m6baN5Cg'
headers = {
    'apikey': key,
    'Authorization': f'Bearer {key}',
    'Content-Type': 'application/json',
    'Prefer': 'return=minimal'
}

with open('recruiting_jobs.csv', 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f)
    rows = list(reader)

print(f"Read {len(rows)} rows from recruiting_jobs.csv")
payload = json.dumps(rows).encode('utf-8')

req = urllib.request.Request(base_url, data=payload, headers=headers, method='POST')
try:
    with urllib.request.urlopen(req) as resp:
        print(f"SUCCESS! Inserted {len(rows)} records into recruiting_jobs! (Status {resp.status})")
except urllib.error.HTTPError as e:
    print(f"HTTPError: {e.code} {e.read().decode('utf-8')}")
except Exception as e:
    print(f"Error: {e}")
