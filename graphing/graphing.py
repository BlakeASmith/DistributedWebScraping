import plotly.express as px
import json
import sys
import datetime
from pathlib import Path
from types import SimpleNamespace
from functools import reduce

data = [SimpleNamespace(**json.loads(text)) for text in Path(sys.argv[1]).read_text().split("\n") if text]
data = sorted(data, key = lambda jb: jb.timestamp)
for ns in data:
    ns.job = SimpleNamespace(**ns.job)

urls = [url for d in data for url in d.job.Urls ]
print(len(urls) == len(set(urls)))


times = [jb.timestamp for jb in data]
mintime = min(times)
adjusted_times = [(tm - mintime)/100000 for tm in times]
print(adjusted_times[:10])
print(sum(adjusted_times) / len(adjusted_times))

processing_time = [jb.numberOfRecordsProduced for jb in data]
records_over_time = []
for i, jb in enumerate(data):
    records_over_time.append(sum(processing_time[:i+1]))


num_records = reduce(lambda a, b: a+b, [jb.numberOfRecordsProduced for jb in data])
num_unreachable = reduce(lambda a, b: a+b, [jb.numberOfUnreachableSites for jb in data])
average_times = [int(jb.processingTimePerPage) for jb in data]
average_proc_time = sum(average_times) / len(data)
total_time = reduce(lambda a, b: a+b, [int(jb.processingTime) for jb in data])

print(f"Number of Records: {num_records}")
print(f"Number of Jobs: {len(data)}")
print(f"Number of Unreachable Sites: {num_unreachable}")
print(f"Average processing time per page: {average_proc_time}ms")
print(f"Total Cumulative Proccessing Time: {datetime.timedelta(milliseconds=total_time)}ms")

fig = px.line(
        title="Two hosts with 3 clients each",
        labels= dict(x="time in seconds", y="number of records"),
        x=adjusted_times, 
        y=records_over_time
    )
#fig.show()
