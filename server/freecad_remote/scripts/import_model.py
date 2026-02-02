import argparse
import sys

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--input", required=True)
    p.add_argument("--format", required=True)
    args = p.parse_args()

    # NOTE: This script is executed by freecadcmd, so FreeCAD modules should be available.
    try:
        import FreeCAD  # type: ignore
        import ImportGui  # type: ignore
    except Exception as e:
        print(f"FreeCAD import failed: {e}", file=sys.stderr)
        sys.exit(2)

    # v0.1: Minimal import. No GUI.
    doc = FreeCAD.newDocument()
    fmt = args.format.lower()

    if fmt in ("stl", "obj", "step", "stp"):
        # ImportGui.insert detects format from file extension in most cases.
        ImportGui.insert(args.input, doc.Name)
    else:
        print(f"Unsupported format: {fmt}", file=sys.stderr)
        sys.exit(3)

    doc.recompute()
    print("ok")
    sys.exit(0)

if __name__ == "__main__":
    main()
