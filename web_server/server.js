var express        = require('express');
var app            = express();
var mongoose       = require('mongoose');
var bodyParser     = require('body-parser');
var methodOverride = require('method-override');
var url            = require('./Config/url');
var logger         = require('morgan');
var port = process.env.PORT || 3333; // set PORT
mongoose.Promise = global.Promise;
mongoose.connect(url.dburl,{ useNewUrlParser: true });

// get all data/stuff of the body (POST) parameters
app.use(logger('tiny'));
app.use(bodyParser.json());
app.use(bodyParser.json({ type: 'application/vnd.api+json' })); // parse application/vnd.api+json as json
app.use(bodyParser.urlencoded({ extended: true })); // parse application/x-www-frorm-urlencoded
app.set('view engine', 'ejs');///////For Testr
app.use(methodOverride('X-HTTP-Method-Override')); // override with the X-HTTP-Method-Override header in the request. simulate DELETE/PUT
app.use(express.static(__dirname + '/views'));

 // set the static files location /public/img will be /img for users
// start app ===============================================
app.use('/css', express.static(__dirname + '/node_modules/bootstrap/dist/css'));
app.use('/js', express.static(__dirname + '/node_modules/bootstrap/dist/js'));

var server = app.listen(port);
server.timeout = 5000;
var io = require('socket.io')(server);

require('./route')(app);
app.set('socketio',io);

console.log('server is on port ' + port);
exports = module.exports = app;
