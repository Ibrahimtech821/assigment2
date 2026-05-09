import json
import pandas as pd
import ast

def convert_cast_and_crew(value):
    if value == "" or pd.isna(value):
        return value

    try:
        value = ast.literal_eval(value)
    except:
        return value

    if isinstance(value, list):
        result = []
        for item in value:
            if isinstance(item, dict):
                name = item.get("name", "")
                role = item.get("description", "")
                result.append(f"{name}-{role}")
        return ", ".join(result)

    return value


with open("mds.json", "r", encoding="utf-8") as file:
    data = json.load(file)

df = pd.json_normalize(data)

main_columns = [
    "name",
    "released_at",
    "genre",
    "streaming_on",
    "country",
    "type",
    "content_rating",
    "imdb_rating",
    "number_of_seasons",
    "cast_and_crew"
]

df = df[main_columns]

df["cast_and_crew"] = df["cast_and_crew"].apply(convert_cast_and_crew)

df.to_csv("mds.csv", index=False, encoding="utf-8-sig")

print("Converted successfully")