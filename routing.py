"""A super simple routing service that will direct android clients to the current cluster leader. 
It's unsecure AF and will need some serious updating later """
import flask
import json
from pathlib import Path

app = flask.Flask("Routing Service")

infoFile = Path("masterinfo.json")

@app.route("/update/<ip>/<port>")
def updateMaster(ip, port):
    with infoFile.open('w') as info:
        json.dump(dict(ip=ip, port=port), info)

    return dict(result=True, address=(ip, port))

@app.route("/masterAddress")
def whoIsMaster():
    with infoFile.open('r') as info:
        return json.load(info)

if __name__ == "__main__":
    app.run(host="0.0.0.0")


