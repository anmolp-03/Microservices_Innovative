FROM golang:1.20-alpine
WORKDIR /app
COPY go.mod go.sum ./
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o review-service ./main.go
EXPOSE 4000
CMD ["/app/review-service"]
