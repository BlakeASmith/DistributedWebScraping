import requests
import zipfile, io

with open("build/client.jar", 'rb') as fp:
    content = fp.read()

resp = requests.post("http://0.0.0.0:5000/plugins/fooplug.jar", data=content)

print(resp.status_code)

# resp = requests.get("http://0.0.0.0:5000/plugins/fooplug.jar")

# z = zipfile.ZipFile(io.BytesIO(resp.content))

# print(z.filelist)

resp = requests.get("http://0.0.0.0:5000/plugins")

print(resp.content)
