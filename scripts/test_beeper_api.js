const readline = require('readline');
const crypto = require('crypto');

const headers = {
    "Authorization": "Bearer BEEPER-PRIVATE-API-PLEASE-DONT-USE",
    "Content-Type": "application/json",
    // Sometimes it helps to have a User-Agent
    "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"
};

async function startLogin(email) {
    const res1 = await fetch("https://api.beeper.com/user/login", {
        method: "POST",
        headers
    });
    console.log("POST /user/login ->", res1.status);
    const body1 = await res1.text();
    console.log(body1);
}

startLogin("iron_feet@hotmail.com");
