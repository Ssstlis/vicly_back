FROM opensuse/leap
MAINTAINER BlessedVictim
RUN  zypper -n install java-1_8_0-openjdk
ADD ./target/universal/backend-0.1.tgz /backend/
EXPOSE 9001
CMD ./backend/backend-0.1/bin/backend
