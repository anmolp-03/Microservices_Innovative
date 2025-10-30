package main

import (
    "database/sql"
    "encoding/json"
    "net/http"
    "os"
    _ "github.com/lib/pq"
)

type Review struct {
    ID int `json:"id"`
    Customer string `json:"customer"`
    Rating int `json:"rating"`
    Comment string `json:"comment"`
}

func main() {
    dbUrl := os.Getenv("REVIEW_DB_URL")
    if dbUrl == "" {
        dbUrl = "postgres://postgres:password@localhost:5432/review?sslmode=disable"
    }
    db, err := sql.Open("postgres", dbUrl)
    if err != nil { panic(err) }

    http.HandleFunc("/reviews", func(w http.ResponseWriter, r *http.Request) {
        if r.Method == "POST" {
            var rev Review
            json.NewDecoder(r.Body).Decode(&rev)
            _, err := db.Exec("INSERT INTO reviews(customer, rating, comment) VALUES($1,$2,$3)", rev.Customer, rev.Rating, rev.Comment)
            if err != nil { http.Error(w, "db error", 500); return }
            w.WriteHeader(201)
            return
        }
        http.Error(w, "only POST", 405)
    })

    http.ListenAndServe(":4000", nil)
}
