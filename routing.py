"""A super simple routing service that will direct android clients to the current cluster leader. 
It's unsecure AF and will need some serious updating later """
import os
import flask
from flask import Flask, request, abort, jsonify, send_from_directory
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


UPLOAD_DIRECTORY = "uploads"
if not os.path.exists(UPLOAD_DIRECTORY):
    os.makedirs(UPLOAD_DIRECTORY)

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


@app.route("/plugins")
def list_files():
    """Endpoint to list files on the server."""
    files = []
    for filename in os.listdir(UPLOAD_DIRECTORY):
        path = os.path.join(UPLOAD_DIRECTORY, filename)
        if os.path.isfile(path):
            files.append(filename)
    return jsonify(files)


@app.route("/plugins/<path:path>")
def get_file(path):
    """Download a file."""
    return send_from_directory(UPLOAD_DIRECTORY, path, as_attachment=True)

@app.route("/plugins/<filename>", methods=["POST"])
def post_file(filename):
    """Upload a file."""

    if "/" in filename:
        # Return 400 BAD REQUEST
        abort(400, "no subdirectories directories allowed")

    with open(os.path.join(UPLOAD_DIRECTORY, filename), "wb") as fp:
        fp.write(request.data)

    # Return 201 CREATED
    return "", 201


if __name__ == "__main__":
    app.run(host="0.0.0.0")
