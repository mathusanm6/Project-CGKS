from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent

# Folder and file convention name
DATA_FOLDER = "/data"
DATASETS_FOLDER = BASE_DIR / "datasets"
SAVED_PIPELINE_FOLDER = BASE_DIR / "pipeline"
PLOT_FOLDER = "plot"

# Pipeline convention name
V_PIPELINE = "v0"
LABEL_NAME: str = "Class"
DROP_COLS = []
ANNOT = {
    'choco':0,
    'spmf':1
}
ANNOT_T = {
    0 : 'choco',
    1: 'spmf'
}

CHOCO_VALUE = "choco"
SPMF_VALUE= "spmf"
