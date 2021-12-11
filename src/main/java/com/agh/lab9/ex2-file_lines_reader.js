var fs = require("fs");
var walk = require("walkdir");
var { performance } = require("perf_hooks");

var global_count = 0;

function read_file_lines(file) {
    var count = 0;
    return new Promise((resolve, reject) => {
        fs.createReadStream(file)
            .on("data", function (chunk) {
                count +=
                    chunk
                        .toString("utf8")
                        .split(/\r\n|[\n\r\u0085\u2028\u2029]/g).length - 1;
            })
            .on("end", function () {
                global_count += count;
                console.log(file, count);
                resolve(count);
            })
            .on("error", function (err) {
                console.error(err);
            });
    });
}

function synchronized_count() {
    walk.sync("./", async function (path, stat) {
        await read_file_lines(path);
    });

    return new Promise((resolve, reject) => {
        resolve();
    });
}

function asynchronized_count() {
    var emitter = walk("./");

    var functions = [];
    emitter.on("file", function (path, stat) {
        functions.push(
            new Promise((resolve, reject) => {
                var count = 0;
                fs.createReadStream(path)
                    .on("data", function (chunk) {
                        count +=
                            chunk
                                .toString("utf8")
                                .split(/\r\n|[\n\r\u0085\u2028\u2029]/g)
                                .length - 1;
                    })
                    .on("end", function () {
                        global_count += count;
                        console.log(path, count);
                        resolve(path);
                    })
                    .on("error", function (err) {
                        console.error(err);
                    });
            })
        );
    });

    Promise.all(functions).then(() => {
        return new Promise((resolve, reject) => {
            resolve();
        });
    });
}

function count_time(start_time) {
    var endTime = performance.now();

    setTimeout(() => {
        console.log(
            `Synchronous call took ${endTime - startTime} milliseconds` +
            // `Asynchronous call took ${endTime - startTime} milliseconds` +
                "\nall lines: " +
                global_count
        );
    }, 2000);
}

var startTime = performance.now();
result = [synchronized_count()];
// result = [asynchronized_count()];

Promise.all(result).then(() => {
    count_time(startTime);
});
