import plotly.express as px
import plotly.graph_objects as go
from plotly.subplots import make_subplots
import json
import sys
import datetime
from pathlib import Path
from types import SimpleNamespace
from functools import reduce
import pandas as pd

def fetch_metadata(filename):
    data = [json.loads(text) for text in Path(filename).read_text().split("\n") if text]
    data = sorted(data, key = lambda jb: jb["timestamp"])
    times = [jb["timestamp"] for jb in data]
    mintime = min(times)
    for d in data:
        d["timestamp"] = (d["timestamp"] - mintime)/60000
    records_produced = [jb["numberOfRecordsProduced"] for jb in data]
    processing_time = [jb["processingTimePerPage"] for jb in data]
    for i, jb in enumerate(data):
        jb["totalRecordsProducedAtTime"] = sum(records_produced[:i+1])
        jb["toatAvgProcessingTime"] = sum(processing_time[:i+1]) / (i+1)
    intranges = list(range(0, 1400, 100))
    ranges = [f"{n}ms" for n in  intranges]

    for jb in data:
        for i, n in enumerate(intranges):
            if jb["processingTimePerPage"] <= n and "processingTimePerPageGroup" not in jb:
                jb["processingTimePerPageGroup"] = ranges[i]
    return pd.DataFrame(data)

def add_traces_for_dataframe(dataframe, fig):
    fig.add_trace(
            go.Scatter(
                x=dataframe["timestamp"], 
                y=dataframe["toatAvgProcessingTime"],
            ),
        row=2, col=1
    )
    fig.add_trace(
        go.Scatter(x=dataframe["timestamp"], y=dataframe["totalRecordsProducedAtTime"]),
        row=1, col=1
    )


with open("graphs.json") as gr:
    graphs = json.load(gr)

for graph in graphs:
    fig = go.Figure()
    fig = make_subplots(rows=2, cols=1,  subplot_titles=("Number of Pages Processed Over Time", "Average Proccessing Time Per Page Over Time")) 
    dataframe = fetch_metadata(graph["filename"])
    hist = px.histogram(dataframe, x="processingTimePerPageGroup", title=f"Processing Time Per Page Across Jobs: {graph['title']}", 
            labels={'processingTimePerPageGroup':"Processing Time Per Page"}, width=1000)
    hist.update_xaxes(categoryorder="category ascending")
    hist.show()

    add_traces_for_dataframe(dataframe, fig)
    fig.update_xaxes(title_text="Real Time in Mins")
    fig.update_yaxes(title_text="Number of Pages Processed", row=1, col=1)
    fig.update_yaxes(title_text="Average Processing Time Per Page (ms)", row=2, col=1)

    fig.update_layout(title_text=f"Word Count on {graph['site']}: {graph['title']}", width=1000, 
            showlegend=False)
    fig.show()
