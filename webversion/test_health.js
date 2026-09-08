async function test() {
  try {
    const res = await fetch('http://localhost:3000/api/ai/health');
    const data = await res.text();
    console.log(data);
  } catch (err) {
    console.error('Fetch error:', err.message);
  }
}
test();
