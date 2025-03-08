FROM amazoncorretto:17-al2023

# Install dependencies for JavaFX/ScalaFX (X11, OpenGL)
RUN yum update -y && yum install -y \
    libGL \
    libX11 \
    libXext \
    libXtst \
    libXrender \
    libXi \
    freetype \
    xorg-x11-server-Xvfb \
    && yum clean all

WORKDIR /app

COPY target/scala-2.13/port-forward-server.jar /app/

VOLUME /app/config

EXPOSE 8090

# Set environment variable for JavaFX
ENV _JAVA_OPTIONS="-Djava.awt.headless=true"

# Create entrypoint script to support both CLI and GUI modes
RUN echo '#!/bin/bash\n\
if [[ "$1" == "--gui" ]]; then\n\
    # Start Xvfb for headless GUI operation\n\
    Xvfb :99 -screen 0 1024x768x24 > /dev/null 2>&1 &\n\
    export DISPLAY=:99\n\
    # Start with GUI\n\
    java -jar port-forward-server.jar --gui\n\
else\n\
    # Start with CLI\n\
    java -jar port-forward-server.jar --cli\n\
fi' > /app/entrypoint.sh && chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]

CMD ["--cli"]