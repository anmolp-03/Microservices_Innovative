package main

import (
    "context"
    "log"
    "os"
    "time"
    
    "go.mongodb.org/mongo-driver/bson"
    "go.mongodb.org/mongo-driver/mongo"
    "go.mongodb.org/mongo-driver/mongo/options"
    "github.com/streadway/amqp"
)

type Review struct {
    ID          string    `bson:"_id,omitempty"`
    OrderID     string    `bson:"orderId"`
    Rating      int       `bson:"rating"`
    Comment     string    `bson:"comment"`
    CreatedAt   time.Time `bson:"createdAt"`
    UpdatedAt   time.Time `bson:"updatedAt"`
}

type ReviewService struct {
    mongoClient *mongo.Client
    db         *mongo.Database
    rabbitConn *amqp.Connection
    rabbitChan *amqp.Channel
}

func NewReviewService() (*ReviewService, error) {
    // Connect to MongoDB
    mongoURI := os.Getenv("MONGODB_URI")
    if mongoURI == "" {
        mongoURI = "mongodb://root:example@mongodb:27017/restaurant?replicaSet=rs0&authSource=admin"
    }
    
    clientOptions := options.Client().ApplyURI(mongoURI)
    client, err := mongo.Connect(context.Background(), clientOptions)
    if err != nil {
        return nil, err
    }

    // Connect to RabbitMQ
    rabbitURL := os.Getenv("RABBITMQ_URL")
    if rabbitURL == "" {
        rabbitURL = "amqp://guest:guest@rabbitmq:5672"
    }
    
    conn, err := amqp.Dial(rabbitURL)
    if err != nil {
        return nil, err
    }

    ch, err := conn.Channel()
    if err != nil {
        return nil, err
    }

    // Setup RabbitMQ exchanges and queues
    err = ch.ExchangeDeclare(
        "review.events",
        "direct",
        true,
        false,
        false,
        false,
        nil,
    )
    if err != nil {
        return nil, err
    }

    _, err = ch.QueueDeclare(
        "review.submitted",
        true,
        false,
        false,
        false,
        nil,
    )
    if err != nil {
        return nil, err
    }

    err = ch.QueueBind(
        "review.submitted",
        "review.submitted",
        "review.events",
        false,
        nil,
    )
    if err != nil {
        return nil, err
    }

    svc := &ReviewService{
        mongoClient: client,
        db:         client.Database("restaurant"),
        rabbitConn: conn,
        rabbitChan: ch,
    }

    // Start change stream
    go svc.startChangeStream()

    return svc, nil
}

func (s *ReviewService) startChangeStream() {
    collection := s.db.Collection("reviews")
    stream, err := collection.Watch(context.Background(), mongo.Pipeline{})
    if err != nil {
        log.Printf("Error setting up change stream: %v", err)
        return
    }
    defer stream.Close(context.Background())

    for stream.Next(context.Background()) {
        var changeEvent bson.M
        if err := stream.Decode(&changeEvent); err != nil {
            log.Printf("Error decoding change event: %v", err)
            continue
        }

        if changeEvent["operationType"] == "insert" {
            s.handleReviewCreated(changeEvent["fullDocument"].(bson.M))
        }
    }
}

func (s *ReviewService) handleReviewCreated(doc bson.M) {
    event := bson.M{
        "id":        doc["_id"],
        "orderId":   doc["orderId"],
        "rating":    doc["rating"],
        "comment":   doc["comment"],
        "createdAt": doc["createdAt"],
    }

    body, err := bson.Marshal(event)
    if err != nil {
        log.Printf("Error marshaling review event: %v", err)
        return
    }

    err = s.rabbitChan.Publish(
        "review.events",
        "review.submitted",
        false,
        false,
        amqp.Publishing{
            ContentType: "application/json",
            Body:       body,
        },
    )
    if err != nil {
        log.Printf("Error publishing review event: %v", err)
    }
}

func (s *ReviewService) CreateReview(review *Review) error {
    review.CreatedAt = time.Now()
    review.UpdatedAt = time.Now()

    _, err := s.db.Collection("reviews").InsertOne(context.Background(), review)
    return err
}

func (s *ReviewService) GetReview(id string) (*Review, error) {
    var review Review
    err := s.db.Collection("reviews_read").FindOne(context.Background(), bson.M{"_id": id}).Decode(&review)
    if err != nil {
        return nil, err
    }
    return &review, nil
}

func (s *ReviewService) GetReviewsByOrder(orderID string) ([]Review, error) {
    cursor, err := s.db.Collection("reviews_read").Find(context.Background(), bson.M{"orderId": orderID})
    if err != nil {
        return nil, err
    }
    defer cursor.Close(context.Background())

    var reviews []Review
    if err = cursor.All(context.Background(), &reviews); err != nil {
        return nil, err
    }
    return reviews, nil
}