# Simple static UI Dockerfile
# Serves the ui directory with a lightweight nginx server
FROM nginx:stable-alpine
LABEL maintainer="devops@example.com"

# Remove default nginx static files
RUN rm -rf /usr/share/nginx/html/*

# Copy UI files
COPY . /usr/share/nginx/html

# Expose port 80 (container)
EXPOSE 80

# Use default nginx entrypoint/cmd
