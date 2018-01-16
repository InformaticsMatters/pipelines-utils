# Pipelines Utils
A utility repository of common **Informatics Matters** _Pipeline_ utilities
as well as an automated [Groovy]-based `PipelineTester` tool that can be used
to verify the behaviour of your pipelines.

## Building the Python pipeline utilities distribution
The Python utilities are built using `setup.py` and distributed with `twine`.
From the `src/python` directory, assuming that you've done the normal
`pip install -r requirmenmts.txt` you can check and then build the
distribution with: -

    pyroma .
    python setup.py bdist_wheel
    
And then upload to PIP with twine (assuming that you have a PIP account
and suitable credentials): -

    twine upload dist/*

---

[Groovy]: http://groovy-lang.org
