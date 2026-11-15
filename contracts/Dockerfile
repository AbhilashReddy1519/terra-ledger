FROM node:20-alpine
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN chmod +x entrypoint.sh && npx hardhat compile
EXPOSE 8545
CMD ["./entrypoint.sh"]
