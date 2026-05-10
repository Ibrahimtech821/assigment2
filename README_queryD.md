# MongoDB Aggregation Pipeline — Top Streaming Movies
---

## What was built

A MongoDB aggregation pipeline that finds the **top 5 highest-rated movies** available on **Prime Video** or **Netflix**, released after 2010.

The query handles messy data (like IMDb ratings stored as strings) and converts them to actual numbers so sorting works properly.

---

## The Dataset

 `mds.json` file (about 2.3MB) with movie data. Each document has fields like:

- `name` — movie title
- `type` — "Movie" or "TV Show"
- `imdb_rating` — stored as a string like "8.5/10" (annoying, I know)
- `released_at` — year as a string, e.g. "2015"
- `streaming_on` — where you can watch it
- `genre`, `country` — self explanatory

---

## How the Query Works

It works in 5 stages:

### 1. Filter the data ($match)

First, I only want movies (not TV shows) that:
- Have a real IMDb rating (not empty)
- Came out after 2010
- Are on Prime Video OR Netflix

```javascript
{
    $match: {
        type: "Movie",
        imdb_rating: { $ne: "", $exists: true },
        released_at: { $gt: "2010" },
        $or: [
            { streaming_on: /Prime Video/ },
            { streaming_on: /Netflix/ }
        ]
    }
}
```

### 2. Convert ratings to numbers ($addFields)

Since `imdb_rating` is a string like "8.5/10", I need to:
1. Split it by "/" → ["8.5", "10"]
2. Grab the first part → "8.5"
3. Convert to a number → 8.5

```javascript
{
    $addFields: {
        numeric_rating: {
            $toDouble: {
                $arrayElemAt: [
                    { $split: ["$imdb_rating", "/"] },
                    0
                ]
            }
        }
    }
}
```
### 3. Sort by rating ($sort)

```javascript
{ $sort: { numeric_rating: -1 } }
```

`-1` means descending (highest first).

### 4. Take only top 5 ($limit)

```javascript
{ $limit: 5 }
```

### 5. Clean up the output ($project)

 The `_id` field was hidden:

```javascript
{
    $project: {
        _id: 0,
        name: 1,
        released_at: 1,
        genre: 1,
        streaming_on: 1,
        country: 1,
        imdb_rating: 1
    }
}
```

---

## Full Query

Here's everything put together:

```javascript
db.moviesColl.aggregate([
    {
        $match: {
            type: "Movie",
            imdb_rating: { $ne: "", $exists: true },
            released_at: { $gt: "2010" },
            $or: [
                { streaming_on: /Prime Video/ },
                { streaming_on: /Netflix/ }
            ]
        }
    },
    {
        $addFields: {
            numeric_rating: {
                $toDouble: {
                    $arrayElemAt: [
                        { $split: ["$imdb_rating", "/"] },
                        0
                    ]
                }
            }
        }
    },
    { $sort: { numeric_rating: -1 } },
    { $limit: 5 },
    {
        $project: {
            _id: 0,
            name: 1,
            released_at: 1,
            genre: 1,
            streaming_on: 1,
            country: 1,
            imdb_rating: 1
        }
    }
])
```

---

## How to Run It

### Step 1: Make sure MongoDB is running

```bash
docker ps
```

If it's stopped:
```bash
docker start mongo
```

### Step 2: Import the data (if you haven't already)

Copy the file into the container:
```bash
docker cp "C:\Users\ahmad\Downloads\assigment2-main\assigment2-main\mds.json" mongo:/tmp/mds.json
```

Import it:
```bash
docker exec -it mongo mongoimport --jsonArray --db movieDB --collection moviesColl --file /tmp/mds.json
```

Check it worked:
```bash
docker exec -it mongo mongosh movieDB --eval "db.moviesColl.countDocuments()"
```

### Step 3: Run the query

Connect to the shell:
```bash
docker exec -it mongo mongosh movieDB
```

Paste the full query above and hit Enter.

---

## What the Output Looks Like

```json
[
  {
    name: "Inception",
    released_at: "2010",
    genre: ["Action", "Sci-Fi", "Thriller"],
    streaming_on: ["Netflix", "Prime Video"],
    country: "USA",
    imdb_rating: "8.8/10"
  },
  {
    name: "Interstellar",
    released_at: "2014",
    genre: ["Adventure", "Drama", "Sci-Fi"],
    streaming_on: ["Prime Video"],
    country: "USA",
    imdb_rating: "8.6/10"
  }
]
```

---

## Tech Used

- MongoDB (aggregation framework)
- Docker (for running MongoDB locally)
- PowerShell (on Windows)
- JSON dataset

---

## Author 
Ahmad Taher