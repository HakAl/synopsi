## Windows

---------------
### Create / Activate Virtual Environment

```
python -m venv myenv
.\myenv\Scripts\Activate.ps1
deactivate
```

### Install Requirements

```
pip install torch
pip freeze | Select-String torch > requirements-example.txt
||
pip install depone deptwo depthree
pip freeze | Select-String "(depone|deptwo|depthree)" > requirements-example.txt
```
### Add Requirement
```
pip freeze | findstr dep4 >> requirements.txt
```

## Bash

---------------
### Create / Activate Virtual Environment

```
python3 -m venv myenv
source myenv/bin/activate
deactivate
```
### Install Requirements

```
pip install torch
pip freeze | grep torch > requirements-example.txt
||
pip install depone deptwo depthree
pip freeze | grep -E "depone|deptwo|depthree" > requirements-example.txt
```

### Add Requirement
```
pip freeze | grep dep4 >> requirements.txt
```