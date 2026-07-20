const loginToken = "88fd828f3c8c06bf5ce7ada73d0af2fa19195dce";

async function loginMatrix() {
    const res = await fetch("https://matrix.beeper.com/_matrix/client/r0/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            type: "m.login.token",
            token: loginToken,
            initial_device_display_name: "Beeper4LightOS"
        })
    });
    
    console.log(res.status);
    const body = await res.json();
    console.log(body);
}

loginMatrix();
