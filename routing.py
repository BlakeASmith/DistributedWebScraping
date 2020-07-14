"""A super simple routing service that will direct android clients to the current cluster leader. 
It's unsecure AF and will need some serious updating later """
import flask
import json
from pathlib import Path

app = flask.Flask("Routing Service")

infoFile = Path("masterinfo.json")

@app.route("/update/<ip>/<port>")
def updateMaster(ip, port):
    info = whoIsMaster()
    with infoFile.open('w') as infof:
        json.dump(info.update({ ip: ip, port:port }), infof)

    return dict(result=True, address=(ip, port))

@app.route("/update/db/<ip>/<port>")
def updateDB(ip, port):
    info = whoIsMaster()
    with infoFile.open('w') as infof:
        json.dump(info.update({ dbip: ip, dbport: port}), infof)

    return dict(result=True, address=(ip, port))

@app.route("/masterAddress")
def whoIsMaster():
    with infoFile.open('r') as info:
        return json.load(info)

if __name__ == "__main__":
    app.run(host="0.0.0.0")


