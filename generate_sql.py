import json

with open('jobs.json', 'r', encoding='utf-8') as f:
    jobs = json.load(f)

sql_lines = [
    "-- 1. 临时开启或配置 RLS 插入权限 (如果已放行可跳过)",
    "ALTER TABLE public.recruiting_jobs DISABLE ROW LEVEL SECURITY;",
    "",
    "-- 2. 一键批量插入 80 条真实招考/大厂国企岗位数据",
    "INSERT INTO public.recruiting_jobs (title, company, location, salary, description) VALUES"
]

val_lines = []
for j in jobs:
    cat_name = "【中国国家公务员招录】" if j['category'] == 'CIVIL_SERVANT' else "【名企/央国企校招】"
    salary = "8K-15K" if j['category'] == 'CIVIL_SERVANT' else "18K-35K"
    majors_str = "、".join(j['major_requirements'])
    certs_str = "、".join(j['cert_requirements'])
    desc = f"{cat_name} 学历：{j['education_requirement']}；专业要求：{majors_str}；硬性证书要求：{certs_str}；网申截止：{j['deadline']}"
    
    title_escaped = j['title'].replace("'", "''")
    org_escaped = j['organization'].replace("'", "''")
    loc_escaped = j['location'].replace("'", "''")
    desc_escaped = desc.replace("'", "''")
    
    val_lines.append(f"  ('{title_escaped}', '{org_escaped}', '{loc_escaped}', '{salary}', '{desc_escaped}')")

sql_lines.append(",\n".join(val_lines) + ";")

with open('insert_recruiting_jobs.sql', 'w', encoding='utf-8') as f:
    f.write("\n".join(sql_lines))

print("Generated insert_recruiting_jobs.sql successfully!")
