var async = require("async");

function printAsync(s, cb) {
    var delay = Math.floor(Math.random() * 1000 + 500);
    setTimeout(function () {
        console.log(s);
        if (cb) cb();
    }, delay);
}

function task(n) {
    return new Promise((resolve, reject) => {
        printAsync(n, function () {
            resolve(n);
        });
    });
}

async.waterfall(
    [
        function (done) {
            done(null, task(1));
        },
        function (n, done) {
            console.log("task", n, "done");
            done(null, task(2));
        },
        function (n, done) {
            console.log("task", n, "done");
            done(null, task(3));
        },
        function (n, done) {
            console.log("task", n, "done");
            console.log("done");
        },
    ],
    function (err) {
        if (err) throw new Error(err);
    }
);

// loop(4);
