"""A super simple routing service that will direct android clients to the current cluster leader. 
It's unsecure AF and will need some serious updating later """
import flask
import json
from pathlib import Path

app = flask.Flask("Routing Service")
infoFile = Path("masterinfo.json")

infoFile.write_text(json.dumps({
    "master_ip": "NOT SET",
    "master_port": "NOT SET",
    "db_ip": "NOT SET",
    "db_port": 9042,
}))

@app.route("/update/<ip>/<port>")
def updateMaster(ip, port):
    info = json.loads(infoFile.read_text())
    info.update({"master_ip": ip, "master_port": port})
    with infoFile.open('w') as infof:
        json.dump(info, infof)

    return dict(result=True, address=(ip, port))


@app.route("/update/db/<ip>/<port>")
def updateDB(ip, port):
    info = json.loads(infoFile.read_text())
    info.update({"db_ip": ip, "db_port": port})
    with infoFile.open('w') as infof:
        json.dump(info, infof)

    return dict(result=True, address=(ip, port))


@app.route("/masterAddress")
def whoIsMaster():
    with infoFile.open('r') as info:
        infod = json.load(info)
        return dict(ip=infod["master_ip"], port=infod["master_port"])


@app.route("/dbAddress")
def whoIsdb():
    with infoFile.open('r') as info:
        infod = json.load(info)
        return dict(ip=infod["db_ip"], port=infod["db_port"])


if __name__ == "__main__":
    app.run(host="0.0.0.0")
