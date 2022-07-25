FROM python:3.9-slim

COPY . /app
WORKDIR /app

RUN pip install --upgrade pip && pip install -r IBA-empty-project/requirements.txt

EXPOSE 8000

ENTRYPOINT ["python3", "IBA-empty-project/manage.py", "runserver", "0.0.0.0:8000"]