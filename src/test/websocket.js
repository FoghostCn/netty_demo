const WebSocket = require('ws');

const ws = new WebSocket('ws://localhost:8080/ws');

ws.on('open', function open() {
  ws.send('something');
});

ws.on('message', function incoming(data) {
  console.log(data);
});

ws.on('pong', function () {
  console.log('pong');
});

ws.on('error', function (e) {
  console.error(e);
});

ws.on('close', function () {
  console.log('close');
});


