import plotly.express as px
import plotly.graph_objects as go
from plotly.subplots import make_subplots
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
adjusted_times = [(tm - mintime)/60000 for tm in times]
print(adjusted_times[:10])
print(sum(adjusted_times) / len(adjusted_times))

records_produced = [jb.numberOfRecordsProduced for jb in data]
processing_time = [jb.processingTimePerPage for jb in data]
records_over_time = []
processing_time_over_time = []
for i, jb in enumerate(data):
    records_over_time.append(sum(records_produced[:i+1]))
    processing_time_over_time.append(sum(processing_time[:1+1]) / (i+1))


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



fig = make_subplots(rows=2, cols=1,  subplot_titles=("Number of Pages Processed Over Time", "Average Proccessing Time Per Page Over Time"), shared_xaxes=True)

fig.add_trace(
        go.Scatter(
            x=adjusted_times, 
            y=records_over_time,
        ),
    row=1, col=1
)

fig.add_trace(
    go.Scatter(x=adjusted_times, y=processing_time_over_time),
    row=2, col=1
)

fig.update_xaxes(title_text="Real Time in Seconds")
fig.update_yaxes(title_text="Number of Pages Processed", row=1, col=1)
fig.update_yaxes(title_text="Average Processing Time Per Page (ms)", row=2, col=1)

fig.update_layout(title_text="Word Count on https://www.merriam-webster.com/ On a Single Machine Running 1 Producer and 3 Clients", width=1000)
fig.show()

