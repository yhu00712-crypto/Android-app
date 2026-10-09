import json
import csv

with open('jobs.json', 'r', encoding='utf-8') as f:
    jobs = json.load(f)

with open('recruiting_jobs.csv', 'w', encoding='utf-8', newline='') as f:
    writer = csv.writer(f)
    writer.writerow(['id', 'title', 'company', 'location', 'salary', 'description'])
    for idx, j in enumerate(jobs, 1):
        cat_name = "【中国国家公务员招录】" if j['category'] == 'CIVIL_SERVANT' else "【名企/央国企校招】"
        salary = "8K-15K" if j['category'] == 'CIVIL_SERVANT' else "18K-35K"
        majors_str = "、".join(j['major_requirements'])
        certs_str = "、".join(j['cert_requirements'])
        desc = f"{cat_name} 学历：{j['education_requirement']}；专业要求：{majors_str}；硬性证书要求：{certs_str}；网申截止：{j['deadline']}"
        writer.writerow([
            idx,
            j['title'],
            j['organization'],
            j['location'],
            salary,
            desc
        ])

print(f"Generated recruiting_jobs.csv with id and {len(jobs)} rows!")
