const { chromium } = require('playwright');
const readline = require('readline');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

function askQuestion(query) {
    return new Promise(resolve => rl.question(query, resolve));
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  // Listen to all network requests
  page.on('request', request => {
    if (request.url().includes('api.beeper.com')) {
      console.log('>>', request.method(), request.url());
      console.log('>> Headers:', request.headers());
      if (request.postData()) {
        console.log('>> Payload:', request.postData());
      }
    }
  });

  page.on('response', async response => {
    if (response.url().includes('api.beeper.com')) {
      console.log('<<', response.status(), response.url());
      try {
        const body = await response.text();
        console.log('<< Body:', body);
      } catch (e) {
        console.log('<< Could not read body');
      }
    }
  });

  console.log("Navigating to chat.beeper.com...");
  await page.goto('https://chat.beeper.com', { waitUntil: 'domcontentloaded' });

  console.log("Waiting 10 seconds for React to mount...");
  await page.waitForTimeout(10000);

  console.log("Filling email...");
  try {
    const emailInput = page.getByRole('textbox');
    await emailInput.waitFor({ timeout: 30000 });
    await emailInput.fill('iron_feet@hotmail.com');
    
    console.log("Clicking Continue...");
    await page.getByRole('button', { name: /continue/i }).click();
    console.log("Clicked Continue, waiting 10 seconds for network requests...");
    
    await page.waitForTimeout(10000);
    
    console.log("Please check your email for the code.");
    const code = await askQuestion("Enter the code from the email: ");
    
    console.log("Filling code...");
    const inputs = await page.getByRole('textbox').all();
    console.log("Found textboxes:", inputs.length);
    if (inputs.length > 0) {
       await inputs[0].fill(code);
    }
    
    await page.getByRole('button', { name: /continue|sign in/i }).click();
    console.log("Clicked Continue (on code screen), waiting 10 seconds for login requests...");
    await page.waitForTimeout(10000);

  } catch (e) {
    console.log("Error finding input or clicking enter", e);
  }

  rl.close();
  await browser.close();
})();
